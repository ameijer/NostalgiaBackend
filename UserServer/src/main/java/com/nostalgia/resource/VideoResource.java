package com.nostalgia.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper; 
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.client.ClientConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
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
import com.nostalgia.LocationRepository;
import com.nostalgia.MediaCollectionRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.VideoRepository;
import com.nostalgia.client.SynchClient;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.LoginResponse;
import com.nostalgia.persistence.model.MediaCollection;
import com.nostalgia.persistence.model.SyncSessionCreateResponse;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Reading;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;

@Path("/api/v0/video")
public class VideoResource {


	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(VideoResource.class);

	private static final String FileDataWorkingDirectory = "/webroot/data";

	private final VideoRepository vidRepo;
	private final UserRepository userRepo;
	private final LocationRepository locRepo;

	private static final ObjectMapper om = new ObjectMapper();
	private SynchClient syncClient;
	private final MediaCollectionRepository collRepo; 


	public VideoResource( UserRepository userRepo, VideoRepository vidRepo, LocationRepository locRepo, MediaCollectionRepository collRepo) {
		this.userRepo = userRepo;
		this.vidRepo = vidRepo;
		this.locRepo = locRepo;
		this.collRepo = collRepo;

	}
	
	@SuppressWarnings("unused")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/id")
	@Timed
	public Video findOne(@QueryParam("vidId") String vidId, @Context HttpServletRequest req) throws Exception{
		
		Video matching = vidRepo.findOneById(vidId);
		
		return matching; 
	}
	
	
	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/addtocoll")
	@Timed
	public List<String> addVideoMeta(List<String> toAddTo, @QueryParam("vidId") String vidId, @Context HttpServletRequest req) throws Exception{
		ArrayList<String> added = new ArrayList<String>();	
		for(String collId : toAddTo){
				MediaCollection toAdd = collRepo.findOneById(collId);
				toAdd.getMatchingVideos().put(vidId, Long.toString(System.currentTimeMillis()));
				JsonDocument add = collRepo.save(toAdd);
				added.add(add.id());
			}
		return added; 
	}
	
	//part 1, metadata is uploaded, in return for a video upload key
	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/new")
	@Timed
	public String addVideoMeta(Video adding, @QueryParam("auto") String auto, @Context HttpServletRequest req) throws Exception{

		//Video adding = om.readValue(addingString, Video.class);
		if(adding == null){
			throw new BadRequestException();
		}

		boolean autoAdd = true;

		
		try {
			autoAdd = Boolean.parseBoolean(auto);
		} catch (Exception e){
			logger.error("error getting auto add boolean, defaulting to auto enabled...", e);
		}

		if(adding.getLocation() == null){
			throw new BadRequestException("location is required");
		}

		User uploader = userRepo.findOneById(adding.getOwnerId());

		if(uploader == null){
			resp.sendError(404, "invalid uploader id specified for video");
			logger.error("no user found for user trying to upload video: " + adding.get_id());
			return null;
		}

		//set pointer to video on the user, subscribe them to it as well
		if(adding.getProperties() == null || adding.getProperties().get("sharing_who") == null){
			throw new BadRequestException("invalid sharing settings");
		}

		String sharing = adding.getProperties().get("sharing_who");
		//find any locations that this video maps to, and add it 

		HashMap<String, KnownLocation> matchingLocs = new HashMap<String, KnownLocation>();
		if(autoAdd){
			matchingLocs = locRepo.findKnownLocationsCoveringPoint(adding.getLocation());
		}


		
		String allVidsString = uploader.findCollection(MediaCollection.PRIVATE, "All My Videos"); 
		
		if(allVidsString != null){
			MediaCollection all =  collRepo.findOneById(allVidsString);
			all.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis()));
			collRepo.save(all);
		}
		
		List<MediaCollection> matchingColls = new ArrayList<MediaCollection>();
		List<String> taggedIds = new ArrayList<String>(); 

			//try and get from video itself
			String rawTagString = adding.getProperties().get("initialTags");
			List<String> reatTags = om.readValue(rawTagString, new TypeReference<List<String>>(){});
			taggedIds.addAll( reatTags);
	
			for(String locId : taggedIds){
				String channel = locId.substring(0, 8);
				if(!matchingLocs.containsKey(channel)){

					KnownLocation userSpecified = locRepo.findOneById(locId);
					if(userSpecified != null){
					matchingLocs.put(channel, userSpecified);
					} else {
						//try to find a media collection
						MediaCollection userColl = collRepo.findOneById(locId);
						if(userColl != null){
							matchingColls.add(userColl); 
						}
						
					}
				}
			}
		

		
		//add pointers from all tagged mediacollections
		for(MediaCollection coll : matchingColls){
			if(coll.getMatchingVideos().keySet().contains(adding.get_id())){
				continue;
			}
			
			coll.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis())); 
			collRepo.save(coll); 
			
		}

		switch(sharing){

		case(Video.WHO_EVERYONE): {
			String pubVidCollId = uploader.getPublicVideoCollId();
			MediaCollection publics = null;
			if(pubVidCollId == null){
				publics = new MediaCollection();
				publics.setName(uploader.get_id() + "_pub");
				publics.setCreatorId(uploader.get_id());
				publics.setVisibility(MediaCollection.PUBLIC);

				uploader.addCollection(publics);

			} else {
				publics =  collRepo.findOneById(pubVidCollId); 
			}

			publics.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis()));
			collRepo.save(publics);

			//for public videos, maintain a reference in the location 
			if(matchingLocs != null && matchingLocs.size() > 0){
				for(KnownLocation loc : matchingLocs.values()){
					
					MediaCollection linked = collRepo.findOneById(loc.getLocationCollections().get("primary")); 
					
					if(linked.getMatchingVideos() == null){
						linked.setMatchingVideos(new HashMap<String, String>());
					}

			
					linked.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis()));
					collRepo.save(linked); 
					JsonDocument saved = locRepo.save(loc);

				}
			} 

			for(KnownLocation loc : matchingLocs.values()){
				String existingLocationCollId = uploader.findCollection(MediaCollection.PRIVATE,uploader.get_id() + ":" + loc.get_id()); 
				//if null, we have no videos here previously
				MediaCollection byLocation = null; 
				if(existingLocationCollId  == null){
					//create new media collection for this location for this user
					byLocation = new MediaCollection();
					byLocation.setName(uploader.get_id() + ":" + loc.get_id());
					byLocation.setCreatorId(uploader.get_id());
					byLocation.setVisibility(MediaCollection.PRIVATE);

					uploader.addCollection(byLocation);
				} else {
					byLocation = collRepo.findOneById(existingLocationCollId); 
				}



				if(byLocation.getMatchingVideos().keySet().contains(adding.get_id())){
					//then this video is already mapped to the location

				} else {
					byLocation.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis())); 
				}
				collRepo.save(byLocation);
			}
			break;
		}
		case(Video.WHO_FRIENDS): {
			String sharedVidCollId = uploader.getSharedVideoCollId();
			MediaCollection shareds = null;
			if(sharedVidCollId == null){
				shareds = new MediaCollection();
				shareds.setName(uploader.get_id() + "_shared");
				shareds.setCreatorId(uploader.get_id());
				shareds.setVisibility(MediaCollection.SHARED);

				uploader.addCollection(shareds);
			} else {
				shareds =  collRepo.findOneById(sharedVidCollId); 
			}

			shareds.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis()));
			collRepo.save(shareds);

			//for public videos, maintain a reference in the location 
			if(matchingLocs == null){
				//set special null location for video
				matchingLocs = new HashMap<String, KnownLocation>();
				KnownLocation nullLoc = new KnownLocation();
				nullLoc.set_id("null_location");
				matchingLocs.put("null_location", nullLoc);
			}

			for(KnownLocation loc : matchingLocs.values()){
				String existingLocationCollId = uploader.findCollection(MediaCollection.SHARED, uploader.get_id() + ":" + loc.get_id()); 
				//if null, we have no videos here previously
				MediaCollection byLocation = null; 
				if(existingLocationCollId  == null){
					//create new media collection for this location for this user
					byLocation = new MediaCollection();
					byLocation.setName(uploader.get_id() + ":" + loc.get_id());
					byLocation.setCreatorId(uploader.get_id());
					byLocation.setVisibility(MediaCollection.SHARED);

					uploader.addCollection(byLocation);
				} else {
					byLocation = collRepo.findOneById(existingLocationCollId); 
				}



				if(byLocation.getMatchingVideos().keySet().contains(adding.get_id())){
					//then this video is already mapped to the location

				} else {
					byLocation.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis())); 
				}
				collRepo.save(byLocation);
			}
			break;
		}
		default:
		case(Video.WHO_PRIVATE):{
			String privateVidCollId = uploader.getPrivateVideoCollId();
			MediaCollection privates = null;
			if(privateVidCollId == null){
				privates = new MediaCollection();
				privates.setName(uploader.get_id() + "_priv");
				privates.setCreatorId(uploader.get_id());
				privates.setVisibility(MediaCollection.PRIVATE);

				uploader.addCollection(privates);
			} else {
				privates =  collRepo.findOneById(privateVidCollId); 
			}

			privates.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis()));
			collRepo.save(privates);

			//for public videos, maintain a reference in the location 
			if(matchingLocs == null){
				//set special null location for video
				matchingLocs = new HashMap<String, KnownLocation>();
				KnownLocation nullLoc = new KnownLocation();
				nullLoc.set_id("null_location");
				matchingLocs.put("null_location", nullLoc);
			}

			for(KnownLocation loc : matchingLocs.values()){
				String existingLocationCollId = uploader.findCollection(MediaCollection.PRIVATE, uploader.get_id() + ":" + loc.get_id()); 
				//if null, we have no videos here previously
				MediaCollection byLocation = null; 
				if(existingLocationCollId  == null){
					//create new media collection for this location for this user
					byLocation = new MediaCollection();
					byLocation.setName(uploader.get_id() + ":" + loc.get_id());
					byLocation.setCreatorId(uploader.get_id());
					byLocation.setVisibility(MediaCollection.PRIVATE);

					uploader.addCollection(byLocation);
				} else {
					byLocation = collRepo.findOneById(existingLocationCollId); 
				}

				if(byLocation.getMatchingVideos().keySet().contains(adding.get_id())){
					//then this video is already mapped to the location

				} else {
					byLocation.getMatchingVideos().put(adding.get_id(), Long.toString(System.currentTimeMillis())); 
				}
				collRepo.save(byLocation);
			}
			break;
		}
		}

		//subscribe the user to their video
		uploader.subscribeToUserChannel(adding.getChannelName());

		userRepo.save(uploader);

		adding.setDateCreated(System.currentTimeMillis());
		adding.setStatus("METAONLY");
		JsonDocument saved = vidRepo.save(adding);
		return adding.get_id();
	}



	
 
}
