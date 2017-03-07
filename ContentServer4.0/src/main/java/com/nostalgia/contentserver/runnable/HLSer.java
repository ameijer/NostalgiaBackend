package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nostalgia.contentserver.FFMPEGController;
import com.nostalgia.contentserver.ShellCallback;
import com.nostalgia.contentserver.StdoutCallback;
import com.nostalgia.contentserver.model.dash.jaxb.AdaptationSetType;
import com.nostalgia.contentserver.model.dash.jaxb.MPDtype;
import com.nostalgia.contentserver.model.dash.jaxb.PeriodType;
import com.nostalgia.contentserver.model.dash.jaxb.RepresentationType;
import com.nostalgia.persistence.model.Video;

public class HLSer implements Runnable{

	public static final Logger logger = LoggerFactory.getLogger(HLSer.class);
	private File sourceFile;

	private final BaselineTranscoder priorStage;
	private final File output;
	private boolean complete = false;
	private boolean skipPrevious;
	private List<String> targets;


	public HLSer(List<String> targetResolutions, File output, BaselineTranscoder transcoder, boolean skip){
		super();
		this.targets = targetResolutions;
		this.output = output;
		this.priorStage = transcoder;
		this.skipPrevious = skip;

	}


	@Override
	public void run() {

		if(!skipPrevious){
			while(!priorStage.isComplete()){
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			sourceFile = priorStage.getOutputFile();
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
				logger.info("File: " + existing.getPath() + " does not exist, encoding...");
				File encoded = this.encodeWithFFMPEG(resolution, sourceFile);
				encodedFiles.add(encoded);
			} else {
				encodedFiles.add(existing);
				logger.info("File: " + existing.getPath() + "exists. Skipping encoding");
			}

			long end = System.currentTimeMillis();

			Duration thisRun = Duration.ofMillis(end -start);
			logger.info("File for representation: " + resolution + " took " + thisRun.toString() + " to encode to pre-dashing birate");

		}

		//delete baseline file, we are done with it
		FileUtils.deleteQuietly(sourceFile);

		complete = true;
		logger.info("dashing complete. Representations dashed:" );
		for(String rep : targets){
			logger.info(rep);
		}

		return;
	}


	public boolean isComplete(){
		return this.complete;
	}

	//size looks like: 320x180
	private synchronized File encodeWithFFMPEG(String size, File sourceFile2) {
		FFMPEGController controller = new FFMPEGController();

		ArrayList<String> cmds = controller.generateFFMPEGCommand(size, sourceFile2, output);

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
		return new File(output, size + ".m3u8");
	}

}
