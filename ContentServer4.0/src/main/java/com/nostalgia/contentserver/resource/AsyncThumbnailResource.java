package com.nostalgia.contentserver.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.nostalgia.contentserver.config.DataConfig;
import com.nostalgia.contentserver.model.dash.jaxb.AdaptationSetType;
import com.nostalgia.contentserver.model.dash.jaxb.MPDtype;
import com.nostalgia.contentserver.model.dash.jaxb.RepresentationType;
import com.nostalgia.contentserver.repository.VideoRepository;
import com.nostalgia.contentserver.runnable.BaselineTranscoder;
import com.nostalgia.contentserver.runnable.HLSer;
import com.nostalgia.contentserver.runnable.MPDMaker;
import com.nostalgia.contentserver.runnable.PipelineScrubber;
import com.nostalgia.contentserver.runnable.ThumbnailMaker;
import com.nostalgia.contentserver.utils.Marshal;
import com.nostalgia.persistence.model.Video;

import io.dropwizard.lifecycle.Managed;


public class AsyncThumbnailResource extends AbstractScheduledService implements Managed {

	public final String FileDataRootDir;
	final static Logger logger = LoggerFactory.getLogger(AsyncThumbnailResource.class);
	private String baseUrl; 

	private boolean running = false; 

	private final VideoRepository vidRepo;

	public AsyncThumbnailResource(VideoRepository contentRepo, DataConfig dataConfig) {
		super();
		this.vidRepo = contentRepo;
		FileDataRootDir = dataConfig.datadir;
		baseUrl = dataConfig.baseurl;
	}


	public List<File> processFile(Video metaData, File original, File thumbnailParentDir) throws Exception {

		ThumbnailMaker maker = new ThumbnailMaker(metaData.get_id(), original, thumbnailParentDir);

		Thread runner = new Thread(maker);

		runner.start();

		runner.join();


		return maker.getOutputFiles(); 

	}

	@Override
	public void start() throws Exception {
		this.startAsync().awaitRunning();

	}

	@Override
	public void stop() throws Exception {
		this.stopAsync().awaitTerminated();

	}

	@Override
	protected synchronized void runOneIteration() throws Exception {
		if(!running){
			running = true;
			try { 
				HashSet<Video> unprocessed = vidRepo.getVideosNeedingThumbs();

				if(unprocessed == null || unprocessed.size() < 1){
					logger.info("no videos found to process, sleeping...");
					return; 
				}

				Video vid = unprocessed.iterator().next();

				if(vid.getThumbNails() == null){
					vid.setThumbNails(new ArrayList<String>());
				}
				vidRepo.save(vid);
				logger.info("generating thumbs video with id: " + vid.get_id());

				String filePath = FileDataRootDir + "/" + vid.get_id() + "/" + vid.get_id();
				File contentPieceOrig = new File(filePath);

				if(!contentPieceOrig.exists()){
					logger.error("error - no matching file found at: " + contentPieceOrig.getAbsolutePath() + " for video with id: " + vid.get_id());
					return;
				}

				//generate dir for thumbs
				File thumbnailParentDir = new File(contentPieceOrig.getParentFile(), "thumbnails");
				thumbnailParentDir.mkdirs(); 

				//otherwise, we know it exists

				List<File> result = processFile(vid, contentPieceOrig, thumbnailParentDir);

				for(File thumb : result){
					vid.getThumbNails().add(baseUrl + vid.get_id() + "/" + thumbnailParentDir.getName() + "/" + thumb.getName());
				}

				if(vid.getUrl() != null && vid.getUrl().length() > 5){
					vid.setStatus("PROCESSED");
				}

				vidRepo.save(vid);

			} finally {
				running = false;
			}
		} else {
			logger.warn("warning - skipping thumbnail creation");
		}
		return; 

	}

	@Override
	protected Scheduler scheduler() {
		return AbstractScheduledService.Scheduler.newFixedRateSchedule(0, 25,
				TimeUnit.SECONDS);
	}

}
