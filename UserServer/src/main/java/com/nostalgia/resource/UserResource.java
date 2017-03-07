package com.nostalgia.resource;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.couchbase.client.java.document.JsonDocument;
import com.google.api.client.auth.openidconnect.IdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.ImmutableList;
import com.nostalgia.ImageDownloaderBase64;
import com.nostalgia.MediaCollectionRepository;
import com.nostalgia.PasswordRepository;
import com.nostalgia.UserRepository;
import com.nostalgia.aws.SignedCookieCreator;
import com.nostalgia.client.IconClient;
import com.nostalgia.client.LambdaEmailRequestClient;
import com.nostalgia.client.SynchClient;
import com.nostalgia.exception.RegistrationException;
import com.nostalgia.persistence.model.LoginResponse;
import com.nostalgia.persistence.model.MediaCollection;
import com.nostalgia.persistence.model.Password;
import com.nostalgia.persistence.model.SyncSessionCreateResponse;
import com.nostalgia.persistence.model.User;
import com.nostalgia.util.PasswordUtils;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Reading;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;
import oauth.AccessToken;
import oauth.AccessTokenRepository;

@Path("/api/v0/user")
public class UserResource {

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

	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	private final UserRepository userRepo;

	private SynchClient syncClient;
	private UserLocationResource userLocRes;
	private final IconClient icCli; 
	private final SignedCookieCreator creator;
	private final MediaCollectionRepository collRepo;
	private final PasswordRepository passRepo;
	private AccessTokenRepository tokenRepo; 
	private final LambdaEmailRequestClient emailCli; 


	public UserResource(LambdaEmailRequestClient emailCli, AccessTokenRepository tokenRepo, UserRepository userRepo, SynchClient syncClient, UserLocationResource userLoc, IconClient icCli, SignedCookieCreator create, MediaCollectionRepository collRepo, PasswordRepository passRepo) {
		this.userRepo = userRepo;
		this.syncClient = syncClient;
		this.userLocRes = userLoc; 
		this.emailCli = emailCli; 
		this.icCli = icCli;
		this.creator = create;
		this.tokenRepo = tokenRepo;
		this.collRepo = collRepo;
		this.passRepo = passRepo;
	}

	private void setNewStreamingTokens(User needsTokens, long tokenExpiryDate) throws Exception{
		if(needsTokens.getStreamTokens() == null){
			needsTokens.setStreamTokens(new HashMap<String, String>());
		}

		//call to aws here if needed for new tokens
		Map<String, String> generated = creator.generateCookies("https://REDACTED.cloudfront.net/*", tokenExpiryDate);

		needsTokens.getStreamTokens().putAll(generated);

		return;
	}

	@SuppressWarnings("unused")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/id")
	@Timed
	public User getUser(@QueryParam("userId") String userId, @Context HttpServletRequest req) throws Exception{
		return userRepo.findOneById(userId);
	}

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/update/{fieldName}")
	@Timed
	public User userAttrUpdate(final Map<String, String> toChange, @QueryParam("userId") String userId, @PathParam("fieldName") String type, @Context HttpServletRequest req) throws Exception{
		//ICON, HOME, EMAIL, NAME, SETTING
		User matching;
		switch(type){
		case "ICON":

			matching = userRepo.findOneById(userId); 

			if(matching == null) throw new NotFoundException("Nos user with id: " + userId + " found");

			String icNew = toChange.get("icon"); 
			if(icNew == null) throw new BadRequestException("no field named icon found"); 

			matching.setIcon(icNew);

			userRepo.save(matching);
			return matching; 

		case "HOME":
			matching = userRepo.findOneById(userId); 

			if(matching == null) throw new NotFoundException("Nos user with id: " + userId + " found");

			String newLocale = toChange.get("home"); 
			if(newLocale == null) throw new BadRequestException("no field named home found"); 

			matching.setHomeRegion(newLocale);

			userRepo.save(matching);
			return matching; 

		case "EMAIL":
			matching = userRepo.findOneById(userId); 

			if(matching == null) throw new NotFoundException("Nos user with id: " + userId + " found");

			String email = toChange.get("email"); 
			if(email == null) throw new BadRequestException("no field named email found"); 

			matching.setEmail(email);

			userRepo.save(matching);
			return matching; 

		case "NAME":
			matching = userRepo.findOneById(userId); 

			if(matching == null) throw new NotFoundException("Nos user with id: " + userId + " found");

			String name = toChange.get("name"); 
			if(name == null) throw new BadRequestException("no field named name found"); 

			matching.setUsername(name);

			userRepo.save(matching);
			return matching; 

		case "SETTING":
			matching = userRepo.findOneById(userId); 

			if(matching == null) throw new NotFoundException("Nos user with id: " + userId + " found");

			if(toChange == null) throw new BadRequestException("no changes found"); 

			matching.getSettings().putAll(toChange);

			userRepo.save(matching);
			return matching; 

		default:
			throw new BadRequestException("invalid field name");


		}


	} 

	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/login")
	@Timed
	public LoginResponse userLogin(final User loggingIn, @QueryParam("password") String pass, @QueryParam("type") String type, @Context HttpServletRequest req) throws Exception{


		if(loggingIn == null){
			throw new BadRequestException();
		}

		LoginResponse response = new LoginResponse();
		//TODO move to service
		if(false /*!mpdReq.getApiKey().equalsIgnoreCase("foo")*/){
			throw new ForbiddenException();
		}

		long time1 = System.currentTimeMillis(); 
		User loggedIn; 
		if(type == null || type.equalsIgnoreCase("app")){
			//lookup via uname/pass
			loggedIn = loginWithPass(loggingIn, pass);

		} else {
			//lookup via token

			switch(type){
			case("facebook"):
				loggedIn = loginWithFacebook(loggingIn);
			break;
			case("google"):
				loggedIn = loginWithGoogle(loggingIn);
			break;
			default:
				logger.error("unable to infer type to login");
				resp.sendError(400, "must specify login type!");
				return null;
			}
		}

		if(loggedIn == null){
			resp.sendError(404, "no user found");
			return null;
		}

		final long loginTime = System.currentTimeMillis() - time1;
		final long time2 = System.currentTimeMillis(); 

		//open session for user's mobile db
		SyncSessionCreateResponse syncResp = syncClient.createSyncSessionFor(loggedIn);

		if(syncResp == null){
			//register user and try again
			syncClient.registerUser(loggedIn); 
			syncResp = syncClient.createSyncSessionFor(loggedIn);
			if(syncResp == null){
				throw new Exception("sync registration failed!");

			}
		}

		loggedIn.setSyncToken(syncResp.getSession_id());

		if(loggedIn.getAuthorizedDevices() == null){
			loggedIn.setAuthorizedDevices(new ArrayList<String>());
			loggedIn.getAuthorizedDevices().addAll(loggingIn.getAuthorizedDevices());
		} else {

			//merge in the device ID
			for(int i = 0; i < loggingIn.getAuthorizedDevices().size(); i++){
				boolean exists = false;
				for(int j = 0; j < loggedIn.getAuthorizedDevices().size(); j ++){
					if(loggingIn.getAuthorizedDevices().get(i).equalsIgnoreCase(loggedIn.getAuthorizedDevices().get(j))){
						exists = true;
						break;
					}
				}

				if(!exists){
					loggedIn.getAuthorizedDevices().add(loggingIn.getAuthorizedDevices().get(i));
				}
			}


		}

		loggedIn.setLastSeen(System.currentTimeMillis());

		response.setSessionTok(syncResp.getSession_id());


		long time = 1451066974000L; 
		final long syncTokTime = System.currentTimeMillis() - time2; 


		long time3 = System.currentTimeMillis(); 
		setNewStreamingTokens(loggedIn, System.currentTimeMillis() + MONTH_IN_MILLIS);

		if(loggingIn.getLastKnownLoc() != null){
			loggedIn.setLastKnownLoc(loggingIn.getLastKnownLoc());
			loggedIn = userLocRes.updateSubscriptions(loggedIn);
		}
		//make sure all video collection is subscribed 
		checkAndSetDefaultCollections(loggedIn, false);

		syncClient.setSyncChannels(loggedIn);
		getToken("USER", pass, loggedIn);

		userRepo.save(loggedIn);

		long collectionsTime = System.currentTimeMillis() - time3; 

		response.setUser(loggedIn);
		return response;

	}

	private void checkAndSetDefaultCollections(User loggedIn, boolean addOfficials) throws Exception{
		String privates = loggedIn.findCollection(MediaCollection.PRIVATE, loggedIn.get_id() + "_priv"); 
		String publics = loggedIn.findCollection(MediaCollection.PUBLIC, loggedIn.get_id() + "_pub");   
		String shareds = loggedIn.findCollection(MediaCollection.SHARED, loggedIn.get_id() + "_shared"); 

		if(privates == null){
			//create new all video collection
			MediaCollection allColl = new MediaCollection();
			allColl.setName(loggedIn.get_id() + "_priv");
			allColl.setCreatorId(loggedIn.get_id());
			allColl.setVisibility(MediaCollection.PRIVATE);
			collRepo.save(allColl);
			loggedIn.addCollection(allColl);
		}

		if(publics == null){
			//create new all video collection
			MediaCollection allColl = new MediaCollection();
			allColl.setName(loggedIn.get_id() + "_pub");
			allColl.setCreatorId(loggedIn.get_id());
			allColl.setVisibility(MediaCollection.PUBLIC);
			collRepo.save(allColl);
			loggedIn.addCollection(allColl);
		}

		if(shareds == null){
			//create new all video collection
			MediaCollection allColl = new MediaCollection();
			allColl.setName(loggedIn.get_id() + "_shared");
			allColl.setCreatorId(loggedIn.get_id());
			allColl.setVisibility(MediaCollection.SHARED);
			collRepo.save(allColl);
			loggedIn.addCollection(allColl);
		}

		//add in collections from nostalgiaofficial
		if(addOfficials){
			List<User> matches = userRepo.findByName("Nostalgia Official");

			if(matches != null && matches.size() > 0){
				User nostalgiaOfficial = matches.get(0);

				for(String key:nostalgiaOfficial.getCollections().keySet()){

					//skip personal collections
					if(key.contains(nostalgiaOfficial.get_id())){
						continue;
					}
					MediaCollection matching = collRepo.findOneById(nostalgiaOfficial.getCollections().get(key));

					//don't require private collections to exist
					if(matching == null){
						logger.error("no collection with id: " + nostalgiaOfficial.getCollections().get(key) + " found, must create this default collection!", new NullPointerException());
						continue;
					}

					if(loggedIn.findCollection(matching.getVisibility(), matching.getName()) != null){
						//we already have this collection
						continue;
					}

					//skip private collections
					if(matching.getVisibility().equals(MediaCollection.PRIVATE)){
						//private collection, make the user a new one of their own

						MediaCollection newPrivate = new MediaCollection();
						newPrivate.setName(matching.getName());
						newPrivate.setCreatorId(loggedIn.get_id());
						newPrivate.setVisibility(MediaCollection.PRIVATE);
						collRepo.save(newPrivate);
						loggedIn.addCollection(newPrivate);
					} else {
						loggedIn.addCollection(matching); 
					}
				}
			}


		}


	}
	private static final String CLIENT_ID = "REDACTED";
	/**
	 * Default JSON factory to use to deserialize JSON.
	 */
	private final JacksonFactory JSON_FACTORY = new JacksonFactory();


	/**
	 * Default HTTP transport to use to make HTTP requests.
	 */
	private HttpTransport TRANSPORT;

	private User loginWithGoogle(User loggingIn) throws Exception {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(TRANSPORT, JSON_FACTORY).setAudience(Arrays.asList(CLIENT_ID)).build();

		// (Receive idTokenString by HTTPS POST)
		GoogleIdToken idToken = verifier.verify(loggingIn.getToken());

		User result = null;
		if (idToken != null) {
			Payload payload = idToken.getPayload();

			//check for existence of token in db 
			try {
				result = userRepo.findOneByAccountToken(payload.getSubject(), "google");
			} catch (Exception e) {

				logger.error("error finding user with token login", e);
				resp.sendError(503, "database error");
				return null;
			}



		} else {
			logger.error("Invalid ID token.");

		}


		return result; 
	}



	public static final String FB_APP_ID = "REDACTED";
	public static final String FB_APP_SECRET = "REDACTED";



	private User loginWithFacebook(User loggingIn) throws IllegalStateException, FacebookException {
		User result = null;
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setDebugEnabled(true);
		configurationBuilder.setOAuthAppId(FB_APP_ID);
		configurationBuilder.setOAuthAppSecret(FB_APP_SECRET);
		configurationBuilder.setAppSecretProofEnabled(false);
		configurationBuilder.setOAuthAccessToken(loggingIn.getToken());
		configurationBuilder
		.setOAuthPermissions("email, id, name, first_name, last_name, gender, picture, verified, locale, generic");
		configurationBuilder.setUseSSL(true);
		configurationBuilder.setJSONStoreEnabled(true);

		// Create configuration and get Facebook instance
		Configuration configuration = configurationBuilder.build();
		FacebookFactory ff = new FacebookFactory(configuration);
		Facebook facebook = ff.getInstance();
		String name = facebook.getName();
		facebook4j.User me = facebook.getMe();

		//check for existence of token in db 
		try {
			result = userRepo.findOneByAccountToken(me.getId(), "facebook");
		} catch (Exception e) {
			logger.error("error finding facebook user with token: " + me.getId());
			return null;
		}

		if(result == null) return null;

		logger.info("logged in with facebook successfully");
		return result; 

	}

	private User loginWithPass(User loggingIn, String provided) throws Exception {
		User result = null;
		List<User> withName = null;
		try {
			withName = userRepo.findByName(loggingIn.getUsername());
		} catch (Exception e) {
			logger.error("error finding user by name", e);
			e.printStackTrace();
			return null;
		}

		if(withName == null){
			return null;
		}

		//check pw
		for(User match:withName){

			if(result != null) break;
			String ptr = match.getPasswordPtr(); 

			Password matching = passRepo.findOneByOwnerId(match.get_id());


			switch(matching.getVersion()){
			case 2:{
				//salted
				if(PasswordUtils.check(provided, matching.getPassword())){
					result = match;
					break;
				}
			}
			break; 

			default:
				if(matching.getPassword().equalsIgnoreCase(provided)){
					//then we have a match
					result = match;
					break;
				}
				break;
			}
		}


		return result;
	}
	private static List<String> allowedGrantTypes = new ArrayList<String>();
	static {
		allowedGrantTypes.add("USER"); 
	}

	public String getToken(String grantType, String password, User gettingToken) throws Exception {
		// Check if the grant type is allowed
		if (!allowedGrantTypes.contains(grantType)) {
			Response response = Response.status(415).build();
			throw new WebApplicationException(response);
		}


		boolean correct = passRepo.checkPassword(gettingToken, password); 

		if (gettingToken == null || !correct) {
			throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
		}

		// User was found, generate a token and return it.
		AccessToken accessToken = tokenRepo.generateNewAccessToken(gettingToken, System.currentTimeMillis());
		gettingToken.setToken(accessToken.getAccess_token_id().toString()); 

		return accessToken.getAccess_token_id().toString();
	}


	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/register")
	@Timed
	public LoginResponse userRegister(User registering, @QueryParam("password") String pass,  @QueryParam("type") String type, @Context HttpServletRequest req) throws Exception{
		boolean success = false;
		User loggedInUser = null;
		LoginResponse response = new LoginResponse();
		if(registering == null){
			throw new BadRequestException();
		}

		try {

			User existing = null;
			if(registering.getToken() != null){

				existing = userRepo.findOneByAccountToken(registering.getToken(), type);

			} else {

				existing = userRepo.findOneByEmail(registering.getEmail());

			}

			if(existing != null){
				resp.sendError(503, "user already exists, please log in");
				return null;
			}

			//user always subscribes to itself
			ArrayList<String> channels = new ArrayList<String>();
			String userChannel = registering.getChannelName();
			channels.add(userChannel);

			registering.setChannels(channels);

			User loggedIn; 
			if(type == null || type.equalsIgnoreCase("app")){
				//lookup via uname/pass
				loggedIn = registerNewUserApp(registering, pass);

			} else {
				//lookup via token

				switch(type){
				case("facebook"):
					try {
						loggedIn = registerNewUserFacebook(registering);
					} catch (RegistrationException e){
						logger.error("error registering user", e);
						resp.sendError(403, e.getMessage());
						return null;
					}
				break;
				case("google"):
					loggedIn = registerNewUserGoogle(registering);
				break;
				default:
					logger.error("unable to infer type to login");
					resp.sendError(400, "must specify login type!");
					return null;
				}
			}

			if(loggedIn == null){
				resp.sendError(404, "no user found");
				return null;
			}

			//open session for user's mobile db
			loggedInUser = loggedIn; 

			if(loggedInUser == null){
				throw new Exception("unable to parse user");
			}

			//Set image
			if(loggedInUser.getIcon() == null){
				String image = null;
				try {
					image = icCli.getIcon(loggedInUser.getUsername());
				} catch (Exception e){
					logger.error("error getting icon", e);
				}

				loggedInUser.setIcon(image);
			} else {
				logger.info("User already had set icon, skipping icon generation. Icon: " + loggedInUser.getIcon());
			}
			//set default settings
			Map<String, String> settings = new HashMap<String, String>();
			settings.put("sharing_who", WHO_EVERYONE);
			settings.put("sharing_when", WHEN_WIFI);
			settings.put("sharing_where", WHERE_EVERYWHERE);
			settings.put("video_sound", SOUND_MUTE);
			this.setNewStreamingTokens(loggedInUser, System.currentTimeMillis() + MONTH_IN_MILLIS);
			loggedInUser.setSettings(settings);

			loggedInUser.setDateJoined(System.currentTimeMillis());
			loggedInUser.setLastSeen(System.currentTimeMillis());
			loggedInUser.subscribeToUserChannel(userChannel);
			SyncSessionCreateResponse syncResp = syncClient.createSyncSessionFor(loggedInUser);

			if(syncResp == null){
				//register user and try again
				syncClient.registerUser(loggedInUser); 
				syncResp = syncClient.createSyncSessionFor(loggedInUser);
				if(syncResp == null){
					throw new Exception("sync registration failed!");

				}
			}

			loggedInUser.setSyncToken(syncResp.getSession_id());


			response.setSessionTok(syncResp.getSession_id());


			if(loggedInUser.getAuthorizedDevices() == null){
				loggedInUser.setAuthorizedDevices(new ArrayList<String>());
				loggedInUser.getAuthorizedDevices().addAll(registering.getAuthorizedDevices());
			} else {

				//merge in the device ID(s)
				for(int i = 0; i < registering.getAuthorizedDevices().size(); i++){
					boolean exists = false;
					for(int j = 0; j < loggedInUser.getAuthorizedDevices().size(); j ++){
						if(registering.getAuthorizedDevices().get(i).equalsIgnoreCase(loggedInUser.getAuthorizedDevices().get(j))){
							exists = true;
							break;
						}
					}

					if(!exists){
						loggedInUser.getAuthorizedDevices().add(registering.getAuthorizedDevices().get(i));
					}
				}


			}

			if(registering.getLastKnownLoc() != null){
				loggedInUser.setLastLocationUpdate(System.currentTimeMillis());
				loggedInUser.setLastKnownLoc(registering.getLastKnownLoc());
				loggedInUser = userLocRes.updateSubscriptions(loggedInUser);
			}
			checkAndSetDefaultCollections(loggedInUser, true);


			success = true;
		} catch (Exception e){
			logger.error("registration error caught", e);
			success = false;
		} finally {

			if(success && loggedInUser != null){


				syncClient.setSyncChannels(loggedInUser);
				getToken("USER", pass, loggedInUser);
				userRepo.save(loggedInUser);
				Thread.sleep(500);
				String code = emailCli.requestEmailVerify(loggedInUser);
				logger.info("code emailed to user " + loggedInUser.get_id() + ": " + code);
			}
		}
		return response;

	}

	private User registerNewUserApp(User registering, String pass) throws Exception{
		String salted = PasswordUtils.getSaltedHash(pass);
		Password newPass = new Password(salted, null, registering.get_id(), new Date(System.currentTimeMillis()).toString());
		newPass.setVersion(2);

		JsonDocument saved = passRepo.save(newPass);

		registering.setPasswordPtr(saved.id());

		return registering; 
	}

	private User registerNewUserGoogle(User toRegister) throws Exception {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(TRANSPORT, JSON_FACTORY).setAudience(Arrays.asList(CLIENT_ID)).build();

		// (Receive idTokenString by HTTPS POST)
		GoogleIdToken idToken = verifier.verify(toRegister.getToken());
		Payload payload = null;
		User result = null;
		if (idToken != null) {
			payload = idToken.getPayload();
		} else {
			logger.error("Invalid ID token.");

		}

		String email = "null@null.com";
		String userName = email.substring(0, email.indexOf('@'));

		Map<String, Object> vals = payload.getUnknownKeys();
		String name = null;
		String firstName = null;
		String lastName = null;
		String locale = null;
		if(vals != null){
			name = vals.get("name").toString();
			firstName = vals.get("given_name").toString();
			lastName = vals.get("family_name").toString();
			locale = vals.get("locale").toString();
		}

		User added = new User();
		added.setEmail(email);
		added.setUsername(name.replaceAll("\\s+",""));
		//added.setUserName(userName);


		//create google account
		added.getAccounts().put(payload.getSubject(), "google");


		return added; 
	}

	private User registerNewUserFacebook(User user) throws Exception {

		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setDebugEnabled(true);
		configurationBuilder.setOAuthAppId(FB_APP_ID);
		configurationBuilder.setOAuthAppSecret(FB_APP_SECRET);
		configurationBuilder.setAppSecretProofEnabled(false);
		configurationBuilder.setOAuthAccessToken(user.getToken());
		configurationBuilder
		.setOAuthPermissions("email, id, name, first_name, last_name, gender, picture, verified, locale, generic");
		configurationBuilder.setUseSSL(true);
		configurationBuilder.setJSONStoreEnabled(true);

		// Create configuration and get Facebook instance
		Configuration configuration = configurationBuilder.build();
		FacebookFactory ff = new FacebookFactory(configuration);
		Facebook facebook = ff.getInstance();
		String name = facebook.getName();
		facebook4j.User me = facebook.getMe();

		me = facebook.getUser(me.getId(), new Reading().fields("email", "last_name", "gender", "first_name", "picture", "name", "locale", "verified"));


		User vueUser = new User();

		String email = me.getEmail();
		String selfDeclaredEmail = user.getEmail();

		if(email == null && selfDeclaredEmail == null){
			throw new RegistrationException("Email Required to register");
		}

		String toSave = "";

		if(email != null){
			toSave = email;
		} else {
			toSave = selfDeclaredEmail; 
		}

		if(!toSave.contains("@") || !toSave.contains(".")){
			throw new RegistrationException("invalid email");
		}

		vueUser.setEmail(toSave);
		vueUser.setUsername(toSave.substring(0, email.indexOf('@')));
	
		if(vueUser.getAccounts() == null){
			HashMap<String, String> map = new HashMap<String, String>();
			vueUser.setAccounts(map);
		}

		vueUser.getAccounts().put(me.getId(), "facebook");


		if(me.getPicture() != null && me.getPicture().getURL() != null){

			//we have a fb picture to use
			ImageDownloaderBase64 imgDL = new ImageDownloaderBase64(me.getPicture().getURL().toString());
			Thread dl = new Thread(imgDL);
			dl.start();
			try {
				dl.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			vueUser.setIcon(imgDL.getEncodedImage());

		}

		return vueUser; 
	}



}
