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
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.contentserver.runnable.BaselineTranscoder;
import com.nostalgia.contentserver.runnable.HLSer;
import com.nostalgia.persistence.model.Video;
import com.nostalgia.persistence.model.VideoTranscodeCallbackPojo;


public class VideoTranscodedCallbackThread extends Thread {

	private final List<String> thumbsSuffix;
	private final String mpdSuffix;
	private String vidId; 

	public VideoTranscodedCallbackThread(List<String>thumbsSuffix, String mpdSuffix, String vidId) {
		super();
		this.thumbsSuffix = thumbsSuffix;
		this.mpdSuffix = mpdSuffix; 
		this.vidId = vidId;
	}


	@Override
	public void run() {

    	HttpClient client = HttpClientBuilder.create().build();
    	CallbackClient cli = null;
    	try {
			cli =  new CallbackClient(client);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
    	VideoTranscodeCallbackPojo pojo = new VideoTranscodeCallbackPojo();
    	pojo.thumbSuffix = thumbsSuffix;
    	pojo.urlSuffix = mpdSuffix;
    	pojo.targetId = vidId; 
    	
    	boolean success = false; 
    	try {
			success = cli.fireCallback(pojo);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
    	
    	if(success){
		try {
			System.out.println("successfully updated video info for video:\n" + (new ObjectMapper()).writeValueAsString(pojo));
		} catch (JsonProcessingException e) {
		
			e.printStackTrace();
		}
    	} else {
    		System.out.println("ERROR FIRING CALLBACK");
    	}
		
		return; 

	}


}
