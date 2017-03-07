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
import java.util.Properties;
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
import org.jets3t.service.security.AWSCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.nostalgia.contentserver.config.DataConfig;
import com.nostalgia.contentserver.config.S3Config;
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


public class AsyncS3UploadResource extends AbstractScheduledService implements Managed {

	public final File fileDataRootDir;
	final static Logger logger = LoggerFactory.getLogger(AsyncS3UploadResource.class);
	private String baseUrl; 

	private BasicAWSCredentials credentials;
	private TransferManager tx;
	private  String bucketName;

	private boolean running = false; 

	private final VideoRepository vidRepo;

	public AsyncS3UploadResource(VideoRepository contentRepo, S3Config s3Config, DataConfig dataConfig) throws Exception {
		super();
		
		this.vidRepo = contentRepo;
		fileDataRootDir = new File(dataConfig.datadir);
		baseUrl = dataConfig.baseurl;

		credentials = new BasicAWSCredentials( "REDACTED", "REDACTED");
		tx = new TransferManager(credentials);
		bucketName = s3Config.bucketName; 

		
		createAmazonS3Bucket();
	}

	private void createAmazonS3Bucket() {
		try {
			if (tx.getAmazonS3Client().doesBucketExist(bucketName) == false) {
				tx.getAmazonS3Client().createBucket(bucketName);
			}
		} catch (AmazonClientException ace) {
			logger.error("error creating bucket", ace);
		}
	}

	@Override
	public void start() throws Exception {
		this.startAsync().awaitRunning();

	}

	@Override
	public void stop() throws Exception {
		this.stopAsync().awaitTerminated();
	}

	private boolean uploadDirToS3(File rootDir){

		MultipleFileUpload myUpload = tx.uploadDirectory(bucketName, "data/" + rootDir.getName() + "/" , rootDir, true);

		try {
			myUpload.waitForCompletion();
		} catch (AmazonClientException | InterruptedException e) {
			logger.error("error waiting for upload completion", e);
			return false;

		}
		return true;	
	}

	@Override
	protected synchronized void runOneIteration() throws Exception {

		HashSet<Video> readyForUpload = vidRepo.findVideosReadyForDeployment();
		
		if(readyForUpload == null){
			return; 
		}
		
		Video toUpload = readyForUpload.iterator().next(); 
		
		File target = new File(fileDataRootDir, toUpload.get_id());
		
		if(!target.exists() || !target.isDirectory()){
			logger.error("target: " + target.getPath() + " does not exist or is not a dir.");
			return;
		}
		
		toUpload.setStatus("DISTRIBUTING");
		vidRepo.save(toUpload);
		
		boolean success = uploadDirToS3(target);
		
		if(!success){
			throw new Exception("UPLOAD TO S3 FAILED");
		} else {
			logger.info("Successfully uploaded dir: " + target + " to aws s3. Deleting dir...");
			boolean result = FileUtils.deleteQuietly(target);
			if(result){
				logger.info("successfully deleted target: " + target.getAbsolutePath());
			} else {
				logger.error("error deleting file");
				throw new Exception("error deleting target: " + target.getAbsolutePath());
			}
		}
		
		
		return; 

	}

	@Override
	protected Scheduler scheduler() {
		return AbstractScheduledService.Scheduler.newFixedRateSchedule(0, 10,
				TimeUnit.SECONDS);
	}

}
