package com.nostalgia;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.nostalgia.persistence.model.IdenticonRequest;
import com.nostalgia.persistence.model.IdenticonResult;
import com.nostalgia.persistence.model.ProcessContentRequestPojo;
import com.nostalgia.persistence.model.ProcessContentResponsePojo;
import com.nostalgia.persistence.model.VideoTranscodeCallbackPojo;

public class IdenticonGenFunction implements RequestHandler<IdenticonRequest, IdenticonResult> {

	@Override
	public IdenticonResult handleRequest(IdenticonRequest input, Context context) {
		ObjectMapper mapper = new ObjectMapper(); 
		long start = System.currentTimeMillis(); 
		IdenticonResult res = new IdenticonResult();
		
		

		try {
		context.getLogger().log("Input: " + mapper.writeValueAsString(input));
	
		
		IconService icSvc = new IconService(new IconServiceConfig());
		
		String encoded = icSvc.getBase64Icon(input.seed);
		res.base64EncIcon = encoded;
		
		
		System.out.println("restuinf result:\n" + mapper.writeValueAsString(res));
		} catch (Exception e){
			System.err.println("ERROR IN ICON GEN FUCNTION:\n" + e);
		}
		
	
		
		if(res.base64EncIcon == null || res.base64EncIcon.length() < 10){
			System.out.println("falling back to generating default icon");
			URL icon = getClass().getClassLoader().getResource("defaultUserIcon.png");
			byte[] defaultEncoded = null;
			try {
				defaultEncoded = Resources.toByteArray(icon);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			res.base64EncIcon = new String(Base64.encodeBase64(defaultEncoded, false, false)); 
		}
		System.out.println("function measured executon time as: " + ((double)(System.currentTimeMillis() - start) / (double) 1000) + "seconds");
		return res; 
	}

}
