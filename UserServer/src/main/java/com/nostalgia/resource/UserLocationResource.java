package com.nostalgia.resource;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

import org.apache.commons.codec.binary.Hex;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nostalgia.LocationRepository;
import com.nostalgia.MediaCollectionRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.VideoRepository;
import com.nostalgia.client.SynchClient;
import com.nostalgia.persistence.model.*;

@Path("/api/v0/user/location")
public class UserLocationResource {


	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(UserLocationResource.class);

	private final UserRepository userRepo;
	private final LocationRepository locRepo;
	private final SynchClient sync;
	private VideoRepository vidRepo;

	private MediaCollectionRepository collRepo;


	public UserLocationResource( UserRepository userRepo, LocationRepository locRepo, VideoRepository vidRepo, SynchClient syncClient, MediaCollectionRepository collRepo) {
		this.userRepo = userRepo;
		this.locRepo = locRepo;
		this.vidRepo = vidRepo;
		this.sync = syncClient;
		this.collRepo = collRepo; 

	}

	public User updateSubscriptions(User hasNewLoc) throws Exception{
		if(hasNewLoc.getLastKnownLoc() == null) return null;
		//all the locations we know
		HashMap<String, KnownLocation> nearbys = locRepo.findKnownLocationsCoveringPoint(hasNewLoc.getLastKnownLoc());

		if(nearbys != null && nearbys.keySet().size() > 0){
			Set<String> clone = new HashSet<String>();
			clone.addAll(nearbys.keySet());

			HashSet<String> vidChannels = new HashSet<String>();
			for(KnownLocation loc: nearbys.values()){

				MediaCollection coll = collRepo.findOneById(loc.getLocationCollections().get("primary"));

				for(String videoId : coll.getMatchingVideos().keySet()){
					String channelName = videoId.substring(0, videoId.indexOf("-"));
					vidChannels.add(channelName);
				}
			}

			hasNewLoc.updateVideoChannels(vidChannels);
		}
		hasNewLoc.updateLocationChannels(nearbys);
		sync.setSyncChannels(hasNewLoc);

		updateLocationSubscriptions(hasNewLoc, nearbys); 


		return hasNewLoc; 
	}

	private void updateLocationSubscriptions(User hasNewLoc, HashMap<String, KnownLocation> nearbys) {
		//loop through the nearbys, adding them to the subscribed locations if they do not exisit in it yet

		for(KnownLocation nearby : nearbys.values()){
			hasNewLoc.subscribeToLocation(nearby.get_id()); 
		}

	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/update")
	@Timed
	public void userLocationUpdate(LocationUpdate newLoc, @QueryParam("userId") String userId, @Context HttpServletRequest req) throws Exception{

		if(newLoc == null){
			throw new BadRequestException();
		}

		User matching = userRepo.findOneById(userId);
		if(matching == null) return;

		matching.setLastKnownLoc(newLoc.getLocation());
		matching.setLastLocationUpdate(System.currentTimeMillis());

		matching = this.updateSubscriptions(matching);


		userRepo.save(matching);

		return;

	}

}
