package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nostalgia.contentserver.FFMPEGController;
import com.nostalgia.contentserver.ShellCallback;
import com.nostalgia.contentserver.StdoutCallback;


public class PipelineScrubber implements Runnable{

	public static final Logger logger = LoggerFactory.getLogger(PipelineScrubber.class);
	private final File contentRoot; 

	private final MPDMaker mpd;
	
	private boolean complete = false;
	private boolean active = false;
	

	public PipelineScrubber(File file, MPDMaker mpdWaiter, boolean active){
		super();
		this.active = active;
		contentRoot = file;

		this.mpd = mpdWaiter;

	}


	@Override
	public void run() {

		if(!active){
			logger.warn("content scrubber deactivated, content @ " + contentRoot.getAbsolutePath() + " will need to be deleted manually" );
			return;
		}
		
		
		while( !mpd.isComplete()){
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	
		}
	
		//Delete everything after we have processed it
		if(contentRoot.exists()){
			logger.info("Deleting directory recursively: " + contentRoot.getAbsolutePath());
			try {
				FileUtils.deleteDirectory(contentRoot);
			} catch (IOException e) {
				
				logger.error("Failed to delete intermediate files in dir: " + contentRoot.getAbsolutePath(), e);
			}
		}
		
		complete = true;

		return;
	}


	public boolean isComplete(){
		return this.complete;
	}

	
}
