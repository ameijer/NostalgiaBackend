package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.FFMPEGController;
import com.nostalgia.ShellCallback;
import com.nostalgia.StdoutCallback;


public class ThumbnailMaker implements Runnable{

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

		//step 2: if no matching file, begin transcoding

		long start = System.currentTimeMillis();

		makeThumbnailsWithFFMPEG(fileName);


		long end = System.currentTimeMillis();

		Duration thisRun = Duration.ofMillis(end -start);
		System.out.println("created thumbnails for video: " + sourceFile.getName() + " in " + thisRun.toString());

		loadCreatedFiles();



		complete = true;
		System.out.println("baseline transcoder complete, ready for next stage of pipeline");
		return;
	}


	public boolean isComplete(){
		return this.complete;
	}


	public ArrayList<File> getOutputFiles(){
		if(!complete){
			System.out.println("tried to fetch thumbnails that werent finished yet!");
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
			System.out.println("Iter: checking file: " + toProcess);
			if(toProcess.getName().contains("thumbnail")){
				this.output.add(toProcess);
			}
		}

	}

	private void makeThumbnailsWithFFMPEG(String vidId) {
		FFMPEGController controller = new FFMPEGController();
		//controller.installBinaries(false);
		Set<ArrayList<String>> thumbCommands = new HashSet<ArrayList<String>>();

		File dir = new File(thumbnailParent, "large");
		dir.mkdirs(); 

		ArrayList<String> cmds1 = controller.generateFFMPEGFullSizeThumbnailCommand(vidId, sourceFile, this.thumbnailParent);

		thumbCommands.add(cmds1);

		File dir2 = new File(thumbnailParent, "medium");
		dir2.mkdirs(); 
		ArrayList<String> cmds2 = controller.generateFFMPEGMediumSizeThumbnailCommand(vidId, sourceFile, this.thumbnailParent);

		thumbCommands.add(cmds2);

		File dir3 = new File(thumbnailParent, "small");
		dir3.mkdirs(); 
		ArrayList<String> cmds3 = controller.generateFFMPEGSmallSizeThumbnailCommand(vidId, sourceFile, this.thumbnailParent);

		thumbCommands.add(cmds3);

		ShellCallback sc = null;
		try {
			sc = new StdoutCallback();
		} catch (SecurityException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for(ArrayList<String> curCmd : thumbCommands){
			System.out.println("running command: " + curCmd.toString());
			try {
				int exit = controller.execProcess(curCmd, sc, sourceFile.getParentFile());
				System.out.println("ran command: " + curCmd.toString() + " with exit code: " + exit);
			} catch (Exception e){
				System.err.println("error in thumb maker: " + e);
				System.err.println("quitting to save $$...");
				System.exit(1);
			}

		}



	}

}
