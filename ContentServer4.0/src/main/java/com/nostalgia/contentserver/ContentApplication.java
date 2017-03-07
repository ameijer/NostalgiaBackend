package com.nostalgia.contentserver;

import java.math.BigInteger;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.contentserver.config.ContentServConfig;
import com.nostalgia.contentserver.config.S3Config;
import com.nostalgia.contentserver.repository.VideoRepository;
import com.nostalgia.contentserver.resource.AsyncHLSerResource;
import com.nostalgia.contentserver.resource.AsyncS3UploadResource;
import com.nostalgia.contentserver.resource.AsyncThumbnailResource;
//import com.nostalgia.contentserver.resource.VideoUploadResource;

import io.dropwizard.Application;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ContentApplication extends Application<ContentServConfig> {

	public static final String NAME = "Content_Server";
	final static Logger logger = LoggerFactory
			.getLogger(ContentApplication.class);

	public static void main(String[] args) throws Exception {
		new ContentApplication().run(args);
	}

	@Override
	public String getName() {
		return NAME;
	}

	private void configureCors(Environment environment) {
		Dynamic filter = environment.servlets().addFilter("CORS",
				CrossOriginFilter.class);
		filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class),
				true, "/*");
		filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
				"GET,PUT,POST,DELETE,OPTIONS");
		filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "GET,PUT,POST,DELETE,OPTIONS");
		filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		filter.setInitParameter(
				CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
		filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER,
				"Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,Cookies");
		filter.setInitParameter("allowCredentials", "true");
		filter.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
	}
	
	
	@Override
	public void initialize(Bootstrap<ContentServConfig> bootstrap) {
	}
	

	
	public static interface MixIn {
        @JsonIgnore
        public void setYear(BigInteger year);
    }

	
	
	@Override
	public void run(ContentServConfig config, Environment environment)
			throws Exception {
		configureCors( environment);
		
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.addMixInAnnotations(XMLGregorianCalendar.class, MixIn.class);
		environment.jersey().register(new JacksonMessageBodyProvider(mapper, environment.getValidator()));
		
		VideoRepository vidRepo = new VideoRepository(config.getVideoCouch());

		AsyncHLSerResource processor = new AsyncHLSerResource(vidRepo, config.getDataConfig());
		AsyncThumbnailResource thumbs = new AsyncThumbnailResource(vidRepo, config.getDataConfig());
		AsyncS3UploadResource s3UL = new AsyncS3UploadResource(vidRepo, new S3Config(), config.getDataConfig());

		environment.lifecycle().manage(processor);
		environment.lifecycle().manage(thumbs);
		environment.lifecycle().manage(s3UL);
	}


}
