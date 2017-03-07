package com.nostalgia.resource;

import com.nostalgia.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Hex;
import org.geojson.Point;
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
import com.nostalgia.UserRepository;
import com.nostalgia.VideoRepository;
import com.nostalgia.client.SynchClient;
import com.nostalgia.persistence.model.*;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Reading;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;

@Path("/api/v0/user/subscribe")
public class SubscriptionResource {


	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionResource.class);

	private final UserRepository userRepo;
	private final LocationRepository locRepo;
	private final SynchClient sync;
	private final MediaCollectionRepository collRepo; 

	public User subscribeToLocation(User wantsSubscription, KnownLocation toSubscribeTo) throws Exception{

		wantsSubscription.subscribeToLocation(toSubscribeTo.get_id());

		sync.setSyncChannels(wantsSubscription);

		userRepo.save(wantsSubscription);
		return wantsSubscription;
	}

	public User unsubscribeFromLocation(User wantsSubscription, String idToRemove) throws Exception{

		wantsSubscription.unsubscribeFromLocation(idToRemove);

		sync.setSyncChannels(wantsSubscription);

		userRepo.save(wantsSubscription);
		return wantsSubscription;
	}
	
	public User subscribeToCollection(User wantsSubscription, MediaCollection toSubscribeTo) throws Exception{

		wantsSubscription.addCollection(toSubscribeTo); 

		sync.setSyncChannels(wantsSubscription);

		userRepo.save(wantsSubscription);
		return wantsSubscription;
	}

	public User unsubscribeFromCollection(User wantsSubscription,MediaCollection toRemove) throws Exception{

		wantsSubscription.removeCollection(toRemove);

		sync.setSyncChannels(wantsSubscription);

		userRepo.save(wantsSubscription);
		return wantsSubscription;
	}



	public SubscriptionResource( UserRepository userRepo, LocationRepository locRepo, SynchClient sync, MediaCollectionRepository collRepo) {
		this.userRepo = userRepo;
		this.locRepo = locRepo;
		this.sync = sync;
		this.collRepo = collRepo; 
		//this.sManager = manager;

	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/remove")
	@Timed
	public String removeLocation(@QueryParam("userId") String userId,  @QueryParam("id") String id, @Context HttpServletRequest req) throws Exception{

		if(id== null){
			throw new BadRequestException("no id specified to add");

		}

		if(userId == null){
			throw new BadRequestException("user id required");
		}


		User adding = userRepo.findOneById(userId);

		if(adding == null){
			throw new NotFoundException("no user found for id");
		}

		
		KnownLocation loc = locRepo.findOneById(id);
		MediaCollection coll = null;
		if(loc == null){
			coll = collRepo.findOneById(id);
			
		}
		
		if(loc != null){
			User subscribed = unsubscribeFromLocation(adding, loc.get_id());
			return id;
		}

		if(coll != null){
			if(coll.getName().contains("_linked")){
				throw new BadRequestException("not allowed to sub to linked colls directly"); 
			}
			User subscribed = unsubscribeFromCollection(adding, coll);
			return id;
		}

		return null;

	}



	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/add")
	@Timed
	public String newLocation(@QueryParam("userId") String userId,  @QueryParam("id") String id,@Context HttpServletRequest req) throws Exception{

		if(id== null){
			throw new BadRequestException("no id specified to add");

		}

		if(userId == null){
			throw new BadRequestException("user id required");
		}


		User adding = userRepo.findOneById(userId);

		if(adding == null){
			throw new NotFoundException("no user found for id");
		}

		KnownLocation loc = locRepo.findOneById(id);
		MediaCollection coll = null;
		if(loc == null){
			coll = collRepo.findOneById(id);
		}

		if(loc == null && coll == null){
			throw new NotFoundException("no resource found for id: " + id);
		}

		if(loc != null){
			User subscribed = subscribeToLocation(adding, loc);
			return id;
		}

		if(coll != null){
			User subscribed = subscribeToCollection(adding, coll);
			return id;
		}

		return null;

	}



}
