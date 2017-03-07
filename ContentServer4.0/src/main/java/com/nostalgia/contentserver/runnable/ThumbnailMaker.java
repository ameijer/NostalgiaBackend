package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.contentserver.FFMPEGController;
import com.nostalgia.contentserver.ShellCallback;
import com.nostalgia.contentserver.StdoutCallback;
import com.nostalgia.contentserver.resource.AsyncHLSerResource;

public class ThumbnailMaker implements Runnable{

	public static final Logger logger = LoggerFactory.getLogger(ThumbnailMaker.class);
	private final File sourceFile;
	private final File thumbnailParent; 
	private final ArrayList<File> output = new ArrayList<File>();
	private boolean complete = false;
	private final String fileName; 

	public ThumbnailMaker(String fileNameNoExt, File tnailSource, File thumbnailParentDir){
		super();
		this.sourceFile = tnailSource;
		this.thumbnailParent = thumbnailParentDir; 
		if(!thumbnailParent.exists()){
			thumbnailParent.mkdirs();
		}
		this.fileName = fileNameNoExt; 
	}


	@Override
	public void run() {

		long start = System.currentTimeMillis();

		makeThumbnailsWithFFMPEG(fileName);


		long end = System.currentTimeMillis();

		Duration thisRun = Duration.ofMillis(end -start);
		logger.info("created thumbnails for video: " + sourceFile.getName() + " in " + thisRun.toString());

		loadCreatedFiles();



		complete = true;
		logger.info("baseline transcoder complete, ready for next stage of pipeline");
		return;
	}


	public boolean isComplete(){
		return this.complete;
	}


	public ArrayList<File> getOutputFiles(){
		if(!complete){
			logger.warn("tried to fetch thumbnails that werent finished yet!");
			return null;
		}

		return this.output;
	}
	
	private void loadCreatedFiles(){
		//iterate through all files with thumbnail in their name in the thumbnail dir 
		String[] extensions = new String[]{"jpg"};
		Iterator<File> iter = FileUtils.iterateFiles(thumbnailParent, extensions, true);

		ObjectMapper mapper = new ObjectMapper();

		while(iter.hasNext()){
			File toProcess= iter.next();
			if(toProcess.getName().contains("thumbnail")){
					this.output.add(toProcess);
			}
		}
		
	}

	private void makeThumbnailsWithFFMPEG(String vidId) {
		FFMPEGController controller = new FFMPEGController();

		ArrayList<String> cmds = controller.generateFFMPEGThumbnailCommand(vidId, sourceFile, this.thumbnailParent);

		ShellCallback sc = null;
		try {
			sc = new StdoutCallback();
		} catch (SecurityException | IOException e1) {
			e1.printStackTrace();
		}

		try {
			int exit = controller.execProcess(cmds, sc, sourceFile.getParentFile());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		
	}

}
