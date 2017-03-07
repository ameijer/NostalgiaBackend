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

public class ContentProcessFunction implements RequestHandler<ProcessContentRequestPojo, ProcessContentResponsePojo> {

	@Override
	public ProcessContentResponsePojo handleRequest(ProcessContentRequestPojo input, Context context) {
		ObjectMapper mapper = new ObjectMapper(); 
		long start = System.currentTimeMillis(); 
		try {
		context.getLogger().log("Input: " + mapper.writeValueAsString(input));
		} catch (Exception e){
			e.printStackTrace();
		}
		
		String contentId = input.id;  
		S3UlDlClient s3Cli = null;
		File parentSaveDir = new File("/tmp");
		
		try {
			s3Cli = new S3UlDlClient(new S3Config());
		} catch (Exception e) {
			System.err.println("failed instantiation of s3 client\n" + e);
		} 

		FFMPEGController installer = new FFMPEGController();

		installer.installBinaries(false);
		
		//get corresponding file from pending folder
		File tempDir = s3Cli.getDirFromPending(contentId, parentSaveDir); 
		File targetFile = new File(tempDir, contentId);

		if(!targetFile.exists()){
			System.err.println("Error - can not find file: " + targetFile.getAbsolutePath() + " downloaded from s3 pending folder");
			return null; 
		}
		
		boolean skipBaseline = false; 
		if(input.mode.equals(ProcessContentRequestPojo.NATIVE)){
			skipBaseline = true; 
		}


		//call HLSer
		AsyncHLSerThread hlser = new AsyncHLSerThread(new TranscodeConfig(), tempDir, targetFile, skipBaseline); 
		hlser.start();

		//call thumbnailer
		AsyncThumbnailThread thumbnailer = new AsyncThumbnailThread(new ThumbnailConfig(), tempDir, targetFile); 
		thumbnailer.start();
		try {
			hlser.join();
			thumbnailer.join(); 
		} catch (Exception e){
			e.printStackTrace();
		}

		ArrayList<String> createdThumbs = thumbnailer.getThumbs();
		System.out.println("created thumbs: " + createdThumbs); 

		VideoTranscodedCallbackThread updatr = new VideoTranscodedCallbackThread(createdThumbs, contentId + "/320x180.m3u8", contentId); 
		
		//upload to data folder
		s3Cli.uploadDirToData(tempDir); 
		
		updatr.start();
		//on successful upload, delete corresponding folder in pending
		s3Cli.deletePendingFile(contentId); 

		//delete temp file
		FileUtils.deleteQuietly(tempDir);

		String retVal = null;
		try {
			retVal = mapper.writeValueAsString(createdThumbs);
		} catch (JsonProcessingException e) {
	
			e.printStackTrace();
		}
		ProcessContentResponsePojo resp = new ProcessContentResponsePojo();
		resp.generated_files = createdThumbs;
		try {
			updatr.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Created thumbs being returned to caller :\n" + retVal);
		System.out.println("function measured executon time as: " + ((double)(System.currentTimeMillis() - start) / (double) 1000) + "seconds");
		
		
		
		
		return resp; 
	}

}
