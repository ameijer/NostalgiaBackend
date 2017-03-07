package com.nostalgia.identicon;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nostalgia.identicon.config.IdenticonConfig;
import com.nostalgia.identicon.resource.IdenticonResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;



public class IdenticonApplication extends Application<IdenticonConfig>{

	public static final String NAME = "IdenticonServer";
	final static Logger logger = LoggerFactory
			.getLogger(IdenticonApplication.class);


	public static void main(String[] args) throws Exception {
		new IdenticonApplication().run(args);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void initialize(Bootstrap<IdenticonConfig> bootstrap) {
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
	
	@Override
	public void run(IdenticonConfig config, Environment environment)
			throws Exception {

		configureCors(environment);
		

		IdenticonResource qrResource = new IdenticonResource(config.getIconGenConfig());

		environment.jersey().register(qrResource);

	}
	
}
