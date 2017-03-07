package com.nostalgia;

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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.Video;


public class VideoObjectUpdaterThread extends Thread {

	private static final String CDN_PREFIX = "https://REDACTED.cloudfront.net/";
	private final List<String> thumbsSuffix;
	private final String mpdSuffix;
	private String vidId;
	private String status = "FAILURE"; 

	public VideoObjectUpdaterThread(List<String>thumbsSuffix, String mpdSuffix, String vidId) {
		super();
		this.thumbsSuffix = thumbsSuffix;
		this.mpdSuffix = mpdSuffix; 
		this.vidId = vidId;
	}


	@Override
	public void run() {

		//init couchbase
		LambdaVideoRepository vidRepo = new LambdaVideoRepository(new CouchbaseConfig()); 
		
		//get video object
		Video toMod = vidRepo.findOneById(vidId);
		
		if(toMod == null){
			System.err.println("ERROR: NO VIDEO OBJECT FOUND. NOT MODIFYING VIDEO OBJECT!!");
			return;
		}
		
		//add COMPLETE url 
		String completeUrl = CDN_PREFIX + mpdSuffix; 
		toMod.setUrl(completeUrl);
		
		//add COMPLETE thumbs
		List<String> thumbs = toMod.getThumbNails();
		
		if(thumbs == null){
			thumbs = new ArrayList<String>();
		}
		
		for(String thumbSuf : thumbsSuffix){
			String completeThumbUrl = CDN_PREFIX + thumbSuf;
			thumbs.add(completeThumbUrl);
		}
		
		toMod.setThumbNails(thumbs);
		
		//enable video
		toMod.setEnabled(true);
		
		//set processed
		toMod.setStatus("PROCESSED");
		
		//save back to db
		try {
			vidRepo.save(toMod);
		} catch (Exception e) {
			System.err.println("error saving video back:\n" + e);
		}
		
		
		try {
			System.out.println("successfully updated video info for video:\n" + (new ObjectMapper()).writeValueAsString(toMod));
		} catch (JsonProcessingException e) {
		
			e.printStackTrace();
		}
		status = "SUCCESS";
		return; 

	}


	public String getStatus() {
		return status; 
	}


}
