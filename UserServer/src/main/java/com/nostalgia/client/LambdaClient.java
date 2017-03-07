package com.nostalgia.client;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.LambdaAPIConfig;
import com.nostalgia.persistence.model.ProcessContentRequestPojo;
import com.nostalgia.persistence.model.ProcessContentResponsePojo;


public class LambdaClient {

	final static Logger logger = LoggerFactory.getLogger(LambdaClient.class);

	final private String lambdaAPIKey = "REDACTED";
	private static final ObjectMapper mapper = new ObjectMapper(); 
	private LambdaAPIConfig config;

	private HttpClient client; 

	public LambdaClient(LambdaAPIConfig config, HttpClient client) throws Exception {
		super();
		this.client = client; 
		this.config = config; 
	}

	public List<String> processVideo(String videoIdToProcess, boolean isNative) throws Exception {
		String URL = "https://REDACTED.amazonaws.com/prod/ContentProcessor"; 
		ProcessContentResponsePojo respObj = null; 
		HttpPost httpPost = null;

		httpPost = new HttpPost(URL);

		ProcessContentRequestPojo input = new ProcessContentRequestPojo();
		input.id = videoIdToProcess;
		
		if(isNative){
			input.mode = ProcessContentRequestPojo.NATIVE;
		} else {
			input.mode = ProcessContentRequestPojo.STANDARD; 
		}
		
		
		String toSend = mapper.writeValueAsString(input);
		try {
			
			httpPost.setEntity(new StringEntity(toSend));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		httpPost.setHeader("x-api-key", this.lambdaAPIKey);


		HttpResponse resp = null;
		try {
			resp = client.execute(httpPost);
		} catch (Exception e1) {
			logger.error("error executing client request");
		} 

		if(resp != null && resp.getEntity()!= null) {
			String contents = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
			respObj = mapper.readValue(contents, ProcessContentResponsePojo.class);
		}

		logger.info("LAMBDA CLIENT FINISHED");
		if(respObj != null){
		return respObj.generated_files; 
		} else return null;

	}



}
