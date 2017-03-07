package com.nostalgia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import org.xml.sax.SAXException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nostalgia.S3Config;

import com.nostalgia.persistence.model.ProcessContentRequestPojo;
import com.nostalgia.persistence.model.ProcessContentResponsePojo;
import com.nostalgia.persistence.model.Video;
import com.nostalgia.persistence.model.VideoTranscodeCallbackPojo;



public class CallbackClient {

	String URL = "https://REDACTED.execute-api.us-east-1.amazonaws.com/prod/VideoObjectUpdater"; 
	final private String lambdaAPIKey = "REDACTED";
	private static final ObjectMapper mapper = new ObjectMapper(); 
	

	private HttpClient client; 

	public CallbackClient(HttpClient client) throws Exception {
		super();
		this.client = client; 
	
	}

	public boolean fireCallback(VideoTranscodeCallbackPojo pojo) throws Exception {

		HttpPost httpPost = null;

		httpPost = new HttpPost(URL);


		
		String toSend = mapper.writeValueAsString(pojo);
		try {
			
			httpPost.setEntity(new StringEntity(toSend));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		httpPost.setHeader("x-api-key", this.lambdaAPIKey);


		HttpResponse resp = null;
		try {
			resp = client.execute(httpPost);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
			return false;
		} catch (IOException e1) {
			e1.printStackTrace();
			return false; 
		}

		return true; 

	}



}
