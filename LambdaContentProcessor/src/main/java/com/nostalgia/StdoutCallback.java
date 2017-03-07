package com.nostalgia;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class StdoutCallback implements ShellCallback
{

//	private static final Logger logger = Logger.getLogger("ffmpeg");
	
	public StdoutCallback() throws SecurityException, IOException{
		super();
//		logger.setUseParentHandlers(false);
//		FileHandler fh;  
//		
//		 // This block configure the logger with handler and formatter  
//        fh = new FileHandler("ffmpeg.log");  
//        logger.addHandler(fh);
//        SimpleFormatter formatter = new SimpleFormatter();  
//        fh.setFormatter(formatter);  
		

		return;
		
	}
	
	public void shellOut(String msg) {
		
		 
//		logger.info(msg);
		System.out.println("FFMPEG: " + msg);
		
	}

	public void processComplete(int exitValue) {
		// TODO Auto-generated method stub
		System.out.println("FFMPEG: process complete, exitvalue: " + exitValue);
	}
	
}