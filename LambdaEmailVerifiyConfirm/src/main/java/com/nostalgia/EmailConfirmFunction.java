package com.nostalgia;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.couchbase.client.java.document.json.JsonArray;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.ProcessContentRequestPojo;
import com.nostalgia.persistence.model.ProcessContentResponsePojo;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.VideoTranscodeCallbackPojo;

public class EmailConfirmFunction {

	public static void handler(InputStream inputStream, OutputStream outputStream, Context context) throws Exception{
		ObjectMapper mapper = new ObjectMapper(); 
		long start = System.currentTimeMillis(); 
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, "UTF-8");
		String theString = writer.toString();

		JSONObject totalInput = new JSONObject(theString);

		System.out.println("totalinput json object" + totalInput.toString());

		String queryParam = totalInput.getString("query-params"); 

		System.out.println("query params: " + queryParam);


		Map<String, String> parsed = parse(queryParam); 


		System.out.println("parsed map is: " + parsed.toString());

		String code = parsed.get("code");

		User user = null; 
		//init couchbase
		LambdaUserRepository vidRepo = null;
		String response = "";

		if(code != null){
			vidRepo = new LambdaUserRepository(new CouchbaseConfig()); 
			//lookup user by param token
			System.out.println("trying to find user with code:" + code);

			int tries = 0;
			int maxTries = 10;
			while(tries < maxTries){
				user = vidRepo.findOneByEmailToken(code); 
				if(user == null){
					System.out.println("no user found on try: " + tries);
					Thread.sleep(600);
				} else {
					System.out.println("user found on try: " + tries);
					break;
				}
				tries++;
			}
		} else {
			response += "(no token supplied) "; 
		}


		if(user == null){
			response += "Error - invalid or expired confirmation link!";
			outputStream.write(response.getBytes("UTF-8"));
			outputStream.flush();
			outputStream.close();
			return;
		} else {
			response += "Success - thanks for validating your email"; 
		}

		System.out.println("Writing output to outputStream at relative time: " + (System.currentTimeMillis() - start) + " ms into function");

		outputStream.write(response.getBytes("UTF-8"));
		outputStream.flush();
		outputStream.close();


		//modify user fields
		user.getSettings().put("EMAIL_STATUS", "CONFIRMED_" + System.currentTimeMillis()); 
		user.setEmailVerified(true);
		vidRepo.save(user); 


		System.out.println("function measured executon time as: " + ((double)(System.currentTimeMillis() - start) / (double) 1000) + "seconds");
		return; 
	}


	private static  Map<String, String> parse(String foo) {
		HashMap<String, String> map = new HashMap<String, String>();
		String foo2 = foo.substring(1, foo.length() - 1);  // hack off braces
		StringTokenizer st = new StringTokenizer(foo2, ",");
		while (st.hasMoreTokens()) {
			String thisToken = st.nextToken();
			StringTokenizer st2 = new StringTokenizer(thisToken, "=");

			map.put(st2.nextToken(), st2.nextToken());
		}

		return map; 
	}

}
