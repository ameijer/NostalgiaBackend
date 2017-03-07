package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import com.nostalgia.FFMPEGController;
import com.nostalgia.ShellCallback;
import com.nostalgia.StdoutCallback;

public class BaselineTranscoder implements Runnable{

	private final File sourceFile;
	private final File output;
	private boolean complete = false;

	public BaselineTranscoder(File source, File parent, String fileName){
		super();
		this.output = new File(parent, fileName);
		this.sourceFile = source;
	}


	@Override
	public void run() {


		//step 1: check for already exisiting file
		if(output.exists()){
			//assume sucessful
			complete = true;
			return;

			//otherwise delete the old file
			//FileUtils.deleteQuietly(output);

		}

		//step 2: if no matching file, begin transcoding

		long start = System.currentTimeMillis();

		String fullpath = output.getAbsolutePath();

		File encoded = this.baselineEncodeWithFFMPEG();


		long end = System.currentTimeMillis();

		Duration thisRun = Duration.ofMillis(end -start);
		System.out.println("baseline for for file: " + this.output.getName() + " took " + thisRun.toString() + " to encode");





		complete = true;
		System.out.println("baseline transcoder complete, ready for next stage of pipeline");
		return;
	}


	public boolean isComplete(){
		return this.complete;
	}


	public File getOutputFile(){
		if(!complete){
			System.out.println("tried to fetch transcoded file that wasn't finished yet!");
			return null;
		}

		return this.output;
	}

	private File baselineEncodeWithFFMPEG() {
		FFMPEGController controller = new FFMPEGController();

		//controller.installBinaries(false);
		
		
		ArrayList<String> cmds = controller.generateFFMPEGBaselineCommand(sourceFile, output);

		ShellCallback sc = null;
		try {
			sc = new StdoutCallback();
		} catch (SecurityException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			int exit = controller.execProcess(cmds, sc, sourceFile.getParentFile());
		} catch (Exception e) {
			System.err.println("error in baseline transcode: " + e);
			System.err.println("quitting to save $$...");
			System.exit(1);
		} 
		return output;
	}


	public File getTargetFile() {

		return sourceFile;
	}

}
