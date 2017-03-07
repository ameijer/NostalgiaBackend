package com.nostalgia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.nostalgia.contentserver.runnable.ThumbnailMaker;

public class AsyncThumbnailThread extends Thread {


	private boolean running = false;
	private final File workingDir;
	private final File sourceFile;
	private final ThumbnailConfig config; 
	private final ArrayList<String> thumbs = new ArrayList<String>(); 
	public ArrayList<String> getThumbs() {
		return thumbs;
	}


	public AsyncThumbnailThread(ThumbnailConfig config, File workingDir, File thumbnailSource) {
		super();
		this.config = config; 
		this.workingDir = workingDir;
		this.sourceFile = thumbnailSource;
	}


	private List<File> processFile(String id, File original, File thumbnailParentDir) throws Exception {

		ThumbnailMaker maker = new ThumbnailMaker(id, original, thumbnailParentDir);

		Thread runner = new Thread(maker);

		runner.start();
		runner.join();


		return maker.getOutputFiles(); 

	}



	@Override
	public void run() {

			try { 

				System.out.println("generating thumbs video with id: " + sourceFile.getName());

				if(!sourceFile.exists()){
					System.err.println("error - no matching file found at: " + sourceFile.getAbsolutePath() + " for video");
					return;
				}

				//generate dir for thumbs
				File thumbnailParentDir = new File(workingDir, "thumbnails");
				thumbnailParentDir.mkdirs(); 

				//otherwise, we know it exists

				List<File> result = null;
				try {
					result = processFile(sourceFile.getName(), sourceFile, thumbnailParentDir);
					System.out.println("Async: files being returned from processfile: " + result);
				} catch (Exception e) {
					System.err.println("Error processing thumbs: " + e);
					return; 
				}

				System.out.println("Async: working dir: " + workingDir);
				
				for(File thumb : result){
					String relative = new File("/tmp").toURI().relativize(thumb.toURI()).getPath();
					thumbs.add(relative);
				}


			} finally {
				running = false;
			}
		
		return; 

	}
	
	
}
