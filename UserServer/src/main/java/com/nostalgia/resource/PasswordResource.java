package com.nostalgia.resource;


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
import javax.ws.rs.NotAuthorizedException;
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
import com.nostalgia.PasswordRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.aws.SignedCookieCreator;
import com.nostalgia.client.IconClient;
import com.nostalgia.client.SynchClient;
import com.nostalgia.exception.RegistrationException;
import com.nostalgia.persistence.model.LoginResponse;
import com.nostalgia.persistence.model.MediaCollection;
import com.nostalgia.persistence.model.Password;
import com.nostalgia.persistence.model.SyncSessionCreateResponse;
import com.nostalgia.persistence.model.User;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Reading;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;

@Path("/api/v0/user/password")
public class PasswordResource {

	public static final String WHO_PRIVATE = "PRIVATE";
	public static final String WHO_FRIENDS = "FRIENDS";
	public static final String WHO_EVERYONE = "EVERYONE";
	public static final long MONTH_IN_MILLIS = 2592000000L;
	public static final String WHEN_NOW = "NOW";
	public static final String WHEN_HOUR = "HOUR";
	public static final String WHEN_DAY = "ONE_DAY";
	public static final String WHEN_WIFI = "WIFI";
	private static final String SOUND_MUTE = "MUTE";
	private static final String SOUND_ENABLED = "ENABLED";
	public static final String WHERE_HERE = "HERE";
	public static final String WHERE_EVERYWHERE = "EVERYWHERE";

	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(PasswordResource.class);

	private final UserRepository userRepo;
	private final PasswordRepository passRepo; 


	public PasswordResource( UserRepository userRepo, PasswordRepository passRepo) {
		this.userRepo = userRepo;
		this.passRepo = passRepo;
	}

	@SuppressWarnings("unused")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/forgot")
	@Timed
	public String userLogin(@QueryParam("email") String email, @Context HttpServletRequest req) throws Exception{
		User matching = userRepo.findOneByEmail(email); 
		
		if(matching == null){
			resp.setStatus(404);
			return "no user found with email: " + email; 
		}
		
		
		resp.setStatus(501);
		return "not yet implemented";

	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/change")
	@Timed
	public String userLogin(String newPassword, @QueryParam("orig") String origPass, @QueryParam("userId")String userId, @Context HttpServletRequest req) throws Exception{
		User matching = userRepo.findOneById(userId); 
		
		if(matching == null){
			resp.setStatus(404);
			return "no user found with id: " + userId; 
		}
		
		
		Password current = passRepo.findOneById(matching.getPasswordPtr());
		
		if(current == null){
			throw new Exception("password not found!");
		}
		
		if(!current.getPassword().equalsIgnoreCase(origPass)){
			throw new NotAuthorizedException("original password doews not match!"); 
		}
		
		String oldDate = current.getDateChanged(); 
		//if we are here, password has passed muster
		current.setDateChanged((new Date(System.currentTimeMillis())).toString());
		current.setPassword(newPassword);
		
		passRepo.save(current); 
		
		
		resp.setStatus(200);
		return "password from: " + oldDate + " changed successfully";

	}
	


}
