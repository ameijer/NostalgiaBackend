package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.nostalgia.FFMPEGController;
import com.nostalgia.ShellCallback;
import com.nostalgia.StdoutCallback;


public class HLSer implements Runnable{

	private File sourceFile;

	private final BaselineTranscoder priorStage;
	private final File output;
	private boolean complete = false;
	private boolean skipPrevious;
	private List<String> targets;


	public HLSer(List<String> targetResolutions, File output, BaselineTranscoder transcoder, boolean skipped){
		super();
		this.targets = targetResolutions;
		this.output = output;
		this.priorStage = transcoder;
		/*this.sourceFile = source;*/
		this.skipPrevious = skipped;

	}


	@Override
	public void run() {

		if(!skipPrevious){
			while(!priorStage.isComplete()){
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sourceFile = priorStage.getOutputFile();
		} else {
			//we skipped it, so the file is the target
			sourceFile = priorStage.getTargetFile();
		}


		//now, in targets we should have every representation type that needs to be created
		ArrayList<File> encodedFiles = new ArrayList<File>();

		//run ffmpeg for each representation
		//this could take some time...
		for(String resolution : targets){

			long start = System.currentTimeMillis();
			File existing = new File(output, resolution + ".m3u8");
			String fullpath = existing.getAbsolutePath();
			boolean exists = existing.exists();
			if(!exists){
				System.out.println("File: " + existing.getPath() + " does not exist, encoding...");
				File encoded = this.encodeWithFFMPEG(resolution, sourceFile);
				encodedFiles.add(encoded);
			} else {
				encodedFiles.add(existing);
				System.out.println("File: " + existing.getPath() + "exists. Skipping encoding");
			}

			long end = System.currentTimeMillis();

			Duration thisRun = Duration.ofMillis(end -start);
			System.out.println("File for representation: " + resolution + " took " + thisRun.toString() + " to encode to pre-dashing birate");

		}

		

		//now, we have  a sequence of encoded files

		//		ManualDashFileSet dash = new ManualDashFileSet(output, encodedFiles);
		//		dash.setVerbose(true);
		//		try {
		//			dash.run();
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		} catch (ExitCodeException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		complete = true;
		System.out.println("hlsing complete. resolutions hls'd:" );
		for(String rep : targets){
			System.out.println(rep);
		}

		return;
	}


	public boolean isComplete(){
		return this.complete;
	}

	//size looks like: 320x180
	private synchronized File encodeWithFFMPEG(String size, File sourceFile2) {
		FFMPEGController controller = new FFMPEGController();
		//controller.installBinaries(false);
		ArrayList<String> cmds = controller.generateFFMPEGCommand(size, sourceFile2, output);

		ShellCallback sc = null;
		try {
			sc = new StdoutCallback();
		} catch (SecurityException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			int exit = controller.execProcess(cmds, sc, sourceFile.getParentFile());
		} catch (Exception e){
			System.err.println("error in hlser: " + e);
			System.err.println("quitting to save $$...");
			System.exit(1);
		}
		return new File(output, size + ".m3u8");
	}

}
