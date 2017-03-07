package com.nostalgia.resource;

import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.couchbase.client.java.document.JsonDocument;
import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nostalgia.ImageDownloaderBase64;
import com.nostalgia.MediaCollectionRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.aws.SignedCookieCreator;
import com.nostalgia.client.SynchClient;
import com.nostalgia.exception.RegistrationException;
import com.nostalgia.persistence.model.LoginResponse;
import com.nostalgia.persistence.model.MediaCollection;
import com.nostalgia.persistence.model.SyncSessionCreateResponse;
import com.nostalgia.persistence.model.User;

@Path("/api/v0/mediacollection")
public class MediaCollectionResource {

	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(MediaCollectionResource.class);

	private final UserRepository userRepo;
	private final MediaCollectionRepository collRepo; 

	private SynchClient syncClient;


	public MediaCollectionResource( UserRepository userRepo, SynchClient syncClient, MediaCollectionRepository medCollRepo) {
		this.userRepo = userRepo;
		this.syncClient = syncClient;
		this.collRepo = medCollRepo;

	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/shared/userops")
	@Timed
	public MediaCollection addCollection(List<String> users, @QueryParam("adderId") String adderId, @QueryParam("collId") String collId, @QueryParam("privelige") String privelige, @Context HttpServletRequest req) throws Exception{

		User adder = userRepo.findOneById(adderId); 

		if(adder == null){
			throw new NotFoundException("user id: " + adderId + " not found"); 
		}


		MediaCollection coll = collRepo.findOneById(collId);

		if(!coll.getCreatorId().equals(adderId) && !coll.getWriters().contains(adderId)){
			throw new ForbiddenException("you do not have sufficient privelidges to add new users to this collection"); 
		}

		if(coll == null){
			throw new BadRequestException("no collection with id: " + collId + " found");
		}

		if(!coll.getVisibility().equalsIgnoreCase("SHARED")){
			throw new BadRequestException("The specified collection: " + coll.getName() + " must be shared in order to modify users");
		}

		//otherwise, we know it is good and is shared

		//for each user provided, add/remove them from the list depending on the privelidge
		ArrayList<User> changedUsers = new ArrayList<User>();
		for(String userId : users){
			User adjusting = userRepo.findOneById(userId);

			if(!adder.getFriends().keySet().contains(adjusting.get_id()) || !adjusting.getFriends().keySet().contains(adder.get_id())){
				throw new ForbiddenException("you can only add your friends to collections");
			}

			boolean changed = false; 
			//level 1: add/subtract
			if(privelige.contains("ADD_AS_READER")){
				changed = true;

				coll.getReaders().add(userId); 



			} else if(privelige.contains("REMOVE_AS_READER")){
				changed = true;
				coll.getReaders().remove(userId);
			}

			if(privelige.contains("ADD_AS_WRITER")){
				changed = true;
				coll.getWriters().add(userId);


			} else if(privelige.contains("REMOVE_AS_WRITER")){
				changed = true;
				coll.getWriters().remove(userId); 
			} 

			if(privelige.contains("REMOVE_ALL_ACCESS")){
				changed = true; 
				coll.getReaders().remove(adjusting.get_id());
				coll.getWriters().remove(adjusting.get_id()); 
			}

			if(!changed) throw new BadRequestException("privelidge field badly formed");

			if(coll.getReaders().contains(adjusting.get_id()) && coll.getWriters().contains(adjusting.get_id()) && coll.getCreatorId().contains(adjusting.get_id())){
				//then this person has no access privelidges, so remove it from their account
				adjusting.removeCollection(coll);
			} else {
				//otherwise add it
				adjusting.addCollection(coll);
			}

			changedUsers.add(adjusting);

		}

		//finally, update the other user objects themselves
		for (User changed: changedUsers){
			syncClient.setSyncChannels(changed); 
			userRepo.save(changed); 
		}

		collRepo.save(coll);

		return coll;

	}


	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/new")
	@Timed
	public MediaCollection addCollection(MediaCollection creating, @QueryParam("creatorToken") String tok, @Context HttpServletRequest req) throws Exception{


		if(tok == null){
			throw new BadRequestException();
		}

		User creator = userRepo.findOneById(creating.getCreatorId());

		MediaCollection existing = collRepo.findOneById(creating.get_id());
		if(existing != null){
			throw new NotAllowedException("collection already exists");
		}

		//point user to new collection (subscription also happens in this step)
		creator.addCollection(creating);

		//update sync channels
		syncClient.setSyncChannels(creator);

		//save both user + collection
		userRepo.save(creator);
		collRepo.save(creating); 

		return creating;

	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/update")
	@Timed
	public MediaCollection updateCollection(MediaCollection updated, @QueryParam("updaterToken") String updaterTok, @Context HttpServletRequest req) throws Exception{

		MediaCollection original = collRepo.findOneById(updated.get_id());



		if(original == null){
			throw new BadRequestException("no exisiting collection found! no action taken");
		}

		//check no change of ownership
		if(!original.getCreatorId().equals(updated.getCreatorId()) && !updaterTok.equals(original.getCreatorId())){
			throw new ForbiddenException("only owner can change collection ownership");


		} 


		if(!original.getCreatorId().equals(updated.getCreatorId()) && !original.getWriters().contains(updated.getCreatorId())){
			throw new BadRequestException("can only change owner to an exisitng writer");
		}



		if(!original.getReaders().equals(updated.getReaders()) || !original.getWriters().equals(updated.getWriters()) ){
			throw new ForbiddenException("cannot change access control this way");
		}

		//check privelgiges
		if(!original.getCreatorId().equalsIgnoreCase(updaterTok) && !original.getWriters().contains(updaterTok)){
			throw new ForbiddenException("insufficient update privelidges"); 
		}

		collRepo.save(updated); 

		return updated; 

	}

	@SuppressWarnings("unused")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/delete")
	@Timed
	public MediaCollection removeCollection(@QueryParam("removerToken") String removerTok, @QueryParam("idToDelete") String idToDel, @Context HttpServletRequest req) throws Exception{


		MediaCollection toRemove = collRepo.findOneById(idToDel);
		User remover = userRepo.findOneById(removerTok);

		if(toRemove == null){
			throw new BadRequestException("no collection with id: " + idToDel + "found");
		}

		if(!toRemove.getCreatorId().equalsIgnoreCase(removerTok)){
			throw new ForbiddenException("only a collection owner can delete it");
		}

		//unsubscribe user
		remover.removeCollection(toRemove);

		//delete from repo
		collRepo.remove(toRemove);

		userRepo.save(remover);
		return toRemove; 

	}

	@SuppressWarnings("unused")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/id")
	@Timed
	public MediaCollection getCollection(@QueryParam("collID") String targetId, @Context HttpServletRequest req) throws Exception{


		MediaCollection found = collRepo.findOneById(targetId);

		return found; 

	}




}
