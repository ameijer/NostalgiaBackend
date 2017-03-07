package com.nostalgia.client;

import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.identitymanagement.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.LambdaAPIConfig;
import com.nostalgia.persistence.model.EmailVerifyRequestPojo;


public class LambdaEmailRequestClient {

	final static Logger logger = LoggerFactory.getLogger(LambdaEmailRequestClient.class);

	final private String lambdaAPIKey = "REDACTED";
	private static final ObjectMapper mapper = new ObjectMapper(); 
	private LambdaAPIConfig config;

	private HttpClient client; 

	public LambdaEmailRequestClient(LambdaAPIConfig config, HttpClient client) throws Exception {
		super();
		this.client = client; 
		this.config = config; 
	}

	public String requestEmailVerify(User toVerify) throws Exception {
		String URL = "https://REDACTED.amazonaws.com/prod/EmailRequestFunction"; 
		HttpPost httpPost = null;

		httpPost = new HttpPost(URL);

		EmailVerifyRequestPojo input = new EmailVerifyRequestPojo();
		input.idOfUserToEmail = toVerify.get_id(); 
		
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

		String contents = null;
				
		if(resp != null && resp.getEntity()!= null) {
			contents = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
			logger.info("String contents returned: " + contents);
		}

		return contents; 

	}



}
