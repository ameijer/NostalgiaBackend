package com.nostalgia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.ProcessContentRequestPojo;
import com.nostalgia.persistence.model.ProcessContentResponsePojo;
import com.nostalgia.persistence.model.VideoTranscodeCallbackPojo;

public class VideoUpdateFunction implements RequestHandler<VideoTranscodeCallbackPojo, String> {

	@Override
	public String handleRequest(VideoTranscodeCallbackPojo input, Context context) {
		ObjectMapper mapper = new ObjectMapper(); 
		long start = System.currentTimeMillis(); 
		try {
		context.getLogger().log("Input: " + mapper.writeValueAsString(input));
		} catch (Exception e){
			e.printStackTrace();
		}

		VideoObjectUpdaterThread updatr = new VideoObjectUpdaterThread(input.thumbSuffix, input.urlSuffix, input.targetId); 
		System.out.println("Starting updater thread...");
		updatr.start();
		
		try {
			updatr.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		String status = updatr.getStatus(); 
		System.out.println("function measured executon time as: " + ((double)(System.currentTimeMillis() - start) / (double) 1000) + "seconds, with status: " + status);
		
		
		
		
		return status; 
	}

}
