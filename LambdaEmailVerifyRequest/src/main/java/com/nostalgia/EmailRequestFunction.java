package com.nostalgia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.EmailVerifyRequestPojo;
import com.nostalgia.persistence.model.ProcessContentRequestPojo;
import com.nostalgia.persistence.model.ProcessContentResponsePojo;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.VideoTranscodeCallbackPojo;

public class EmailRequestFunction implements RequestHandler<EmailVerifyRequestPojo, String> {

	private static final String verifyPrefix = "https://REDACTED.amazonaws.com/prod/emailVerifyConfirm";
	LambdaUserRepository userRepo = new LambdaUserRepository(new CouchbaseConfig()); 

	@Override
	public String handleRequest(EmailVerifyRequestPojo input, Context context) {
		ObjectMapper mapper = new ObjectMapper(); 
		long start = System.currentTimeMillis(); 
		try {
			context.getLogger().log("Input: " + mapper.writeValueAsString(input));
		} catch (Exception e){
			e.printStackTrace();
		}
		String code = DigestUtils.sha1Hex(UUID.randomUUID().toString()); 


		String verifyURL = verifyPrefix + "?code=" + code; 




		//init couchbase

		int tries = 0;
		int maxTries = 10;
		User toEmail = null;
		while(tries < maxTries){
			toEmail = userRepo.findOneById(input.idOfUserToEmail); 
			if(toEmail == null){
				try {
					Thread.sleep(1000);
					System.out.println("no user found on try: " + tries + ", waiting one second then trying again");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else break;
			tries++;
		}

		if(toEmail == null){
			System.out.println("No user found matching id: " + input.idOfUserToEmail);
			return null;
		}

		System.out.println("About to email user: " + toEmail.getUsername() + " with url: " + verifyURL);

		boolean success = verifyEmail(toEmail, verifyURL);


		if(success){
			//update user only if the email was send successfully
			toEmail.getSettings().put("EMAIL_CODE", code); 
			toEmail.getSettings().put("last_email", Long.toString(System.currentTimeMillis())); 
			try {
				userRepo.save(toEmail);
			} catch (Exception e) {
				System.out.println("error saving updated user: " + e);
				e.printStackTrace();
			} 
		} else {
			System.out.println("email failed to send, not updating user");
		}

		System.out.println("function measured executon time as: " + ((double)(System.currentTimeMillis() - start) / (double) 1000));


		return code; 
	}

	private boolean verifyEmail(User toEmail, String verifyURL) {
		String to = toEmail.getEmail();
		String from = "noreply@vuescape.io"; 
		String subject = "Nostalgia Email Verification";
		String message = "Hello - please verify your Nostalgia email by clicking on the following link: " + verifyURL; 
		return email(to, from, subject, message); 

	}

	private boolean email(String to, String from, String subject, String message) {
		AwsSESEmailer sender = new AwsSESEmailer(to, from, message, subject);
		System.out.println("email object instantiated. sending...");
		boolean result = false;

		try {
			result = sender.sendEmail();
		} catch (Exception e){
			System.err.println("exception in emailer: " + e.toString());
		}

		if(!result){
			System.out.println("sending message failed!");
		} else {
			System.out.println("sending message successful!");
		}
		return result;
	}

}
