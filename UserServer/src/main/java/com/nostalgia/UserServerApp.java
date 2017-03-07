package com.nostalgia;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.ws.rs.client.Client;

import org.apache.http.client.HttpClient;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilderSpec;
import com.nostalgia.aws.AWSConfig;
import com.nostalgia.aws.SignedCookieCreator;
import com.nostalgia.client.AtomicOpsClient;
import com.nostalgia.client.IconClient;
import com.nostalgia.client.LambdaClient;
import com.nostalgia.client.LambdaEmailRequestClient;
import com.nostalgia.client.S3UploadClient;
import com.nostalgia.client.SynchClient;
import com.nostalgia.persistence.model.User;
import com.nostalgia.resource.AtomicOpsResource;
import com.nostalgia.resource.FriendsResource;
import com.nostalgia.resource.LocationAdminResource;
import com.nostalgia.resource.LocationQueryResource;
import com.nostalgia.resource.SubscriptionResource;
import com.nostalgia.resource.MediaCollectionResource;
import com.nostalgia.resource.PasswordResource;
import com.nostalgia.resource.UserLocationResource;
import com.nostalgia.resource.UserResource;
import com.nostalgia.resource.VideoResource;
import com.nostalgia.resource.VideoUploadResource;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import oauth.AccessToken;
import oauth.AccessTokenRepository;
import oauth.AuthResource;
import oauth.SimpleAuthenticator;
import oauth.SimpleAuthorizer;



public class UserServerApp extends Application<UserAppConfig>{

	public static final String NAME = "Nostalgia";
	final static Logger logger = LoggerFactory
			.getLogger(UserServerApp.class);


	public static void main(String[] args) throws Exception {
		new UserServerApp().run(args);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void initialize(Bootstrap<UserAppConfig> bootstrap) {
		//bootstrap.addBundle(new AssetsBundle(, ));
	}

	private void configureCors(Environment environment) {
		Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
		filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
		filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS");
		filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
		filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
		filter.setInitParameter("allowCredentials", "true");
	}

	private UserRepository getUserRepo(UserAppConfig config, Environment environment){
		UserRepository repo = new UserRepository(config.getUserServerConfig());

		return repo;
	}

	private AccessTokenRepository getTokenRepo(UserAppConfig config, Environment environment){
		AccessTokenRepository repo = new AccessTokenRepository(config.getTokenRepoConfig());

		return repo;
	}
	
	private PasswordRepository getPasswordRepo(UserAppConfig config, Environment environment){
		PasswordRepository repo = new PasswordRepository(config.getPasswordServerConfig());

		return repo;
	}
	
	private LocationRepository getLocationRepo(UserAppConfig config, Environment environment){
		LocationRepository repo = new LocationRepository(config.getLocationServerConfig());

		return repo;
	}
	
	private MediaCollectionRepository getCollectionRepo(UserAppConfig config, Environment environment){
		MediaCollectionRepository repo = new MediaCollectionRepository(config.getCollectionServerConfig());

		return repo;
	}


	public IconClient getIconService(UserAppConfig config, Environment environment){
		logger.info("creating icon server client...");
		final HttpClient httpClient = new HttpClientBuilder(environment).using(config.getHttpClientConfiguration()).build("icon-client");
		IconClient iCli = null;
		try {
			iCli = new IconClient(new LambdaAPIConfig(), httpClient);
		} catch (Exception e) {
			logger.error("ERROR creating icon client", e);
		}

		return iCli;
	}
	
	public LambdaEmailRequestClient getEmailClient(UserAppConfig config, Environment environment){
		logger.info("creating email request client...");
		final HttpClient httpClient = new HttpClientBuilder(environment).using(config.getHttpClientConfiguration()).build("email-client");
		LambdaEmailRequestClient iCli = null;
		try {
			iCli = new LambdaEmailRequestClient(new LambdaAPIConfig(), httpClient);
		} catch (Exception e) {
			logger.error("ERROR creating icon client", e);
		}

		return iCli;
	}
	
	public SynchClient createSynchClient(UserAppConfig config, Environment environment){
		logger.info("creating synch server client...");
		final Client jClient = new JerseyClientBuilder(environment).using(
				config.getJerseyClientConfiguration()).build("sync communicator");
		SynchClient comms = new SynchClient(config.getSyncConfig(), jClient);

		return comms;
	}
	
	public LambdaClient createLambdaClient(UserAppConfig config, Environment environment) throws Exception{
		logger.info("creating aws lambda client...");
		final HttpClient httpClient = new HttpClientBuilder(environment).using(config.getHttpClientConfiguration()).build("lambda-client");
		LambdaClient lCli = new LambdaClient(new LambdaAPIConfig(), httpClient);

		return lCli;
	}

	@Override
	public void run(UserAppConfig config, Environment environment) throws Exception {
		configureCors(environment);

		UserRepository userRepo = this.getUserRepo(config, environment);
		LocationRepository locRepo = this.getLocationRepo(config, environment);
		VideoRepository vidRepo = this.getVideoRepository(config, environment);
		MediaCollectionRepository collRepo =this.getCollectionRepo(config, environment);
		PasswordRepository passRepo = this.getPasswordRepo(config, environment);
		AccessTokenRepository tokenRepo = this.getTokenRepo(config, environment); 
	
		SynchClient sCli = this.createSynchClient(config, environment);
		AtomicOpsClient atomicCli = new AtomicOpsClient(config.getAtomicsServerConfig());
		IconClient icSvc = this.getIconService(config, environment);
		LambdaEmailRequestClient emailCli = this.getEmailClient(config, environment); 
		SignedCookieCreator create = new SignedCookieCreator(new AWSConfig());
		S3UploadClient s3Cli = new S3UploadClient(new S3Config()); 
		LambdaClient lCli = this.createLambdaClient(config, environment); 
		environment.lifecycle().manage(s3Cli);
		
		UserLocationResource locRes = new UserLocationResource(userRepo, locRepo, vidRepo, sCli, collRepo);
		UserResource userResource = new UserResource(emailCli, tokenRepo, userRepo, sCli, locRes, icSvc, create, collRepo, passRepo);
		PasswordResource passRes = new PasswordResource(userRepo, passRepo);
		VideoResource vidRes = new VideoResource(userRepo, vidRepo, locRepo, collRepo);
		LocationAdminResource locCRUD = new LocationAdminResource(  userRepo, locRepo, vidRepo, collRepo);
		LocationQueryResource queryRes = new LocationQueryResource(locRepo);
		SubscriptionResource locSubRes = new SubscriptionResource(userRepo, locRepo, sCli, collRepo);
		FriendsResource friendRes = new FriendsResource(userRepo, sCli);
		MediaCollectionResource collRes = new MediaCollectionResource(userRepo, sCli, collRepo);
		AtomicOpsResource aOps = new AtomicOpsResource(userRepo, atomicCli,  collRepo, vidRepo,  locRepo);
		VideoUploadResource ulRes = new VideoUploadResource(vidRepo, s3Cli, lCli);
		SimpleAuthenticator authr = new SimpleAuthenticator(tokenRepo); 
		CachingAuthenticator<String, AccessToken> cachingAuthenticator = new CachingAuthenticator<>(
                environment.metrics(), authr,
                CacheBuilderSpec.parse("maximumSize=10000, expireAfterAccess=100m"));
		
		environment.jersey().register(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<AccessToken>()
                .setAuthenticator(cachingAuthenticator)
                .setAuthorizer(new SimpleAuthorizer())
                .setPrefix("Bearer")
                .setRealm("Nostalgia")
                .buildAuthFilter()));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<AccessToken>(AccessToken.class));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
		
		environment.jersey().register(passRes);
		environment.jersey().register(ulRes);
		environment.jersey().register(aOps);
		environment.jersey().register(collRes);
		environment.jersey().register(friendRes);
		environment.jersey().register(locSubRes); 
		environment.jersey().register(queryRes);
		environment.jersey().register(locCRUD);
		environment.jersey().register(vidRes);
		environment.jersey().register(locRes);
		environment.jersey().register(userResource);
		environment.jersey().register(new AuthResource(userRepo));

	}

	
	
	private VideoRepository getVideoRepository(UserAppConfig config, Environment environment) {
		VideoRepository repo = new VideoRepository(config.getVideoCouchConfig());
		return repo;
	}

}
