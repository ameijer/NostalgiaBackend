package com.nostalgia.contentserver.runnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.nostalgia.contentserver.FFMPEGController;
import com.nostalgia.contentserver.ShellCallback;
import com.nostalgia.contentserver.StdoutCallback;
import com.nostalgia.contentserver.model.dash.jaxb.BaseURLType;
import com.nostalgia.contentserver.model.dash.jaxb.MPDtype;
import com.nostalgia.contentserver.repository.VideoRepository;
import com.nostalgia.contentserver.utils.Marshal;
import com.nostalgia.persistence.model.Video;

public class MPDMaker implements Runnable{

	public static final Logger logger = LoggerFactory.getLogger(MPDMaker.class);

	private  Video needMPD;
	private  HLSer waitingOn;

	private boolean wait = true;
	private boolean complete = false; 
	private final File videoRoot;
	private final String baseurl;


	public MPDMaker(Video needMPD, HLSer waitingOn, File videoRoot, boolean b, String baseUrl) {
		super();
		this.needMPD = needMPD;
		this.waitingOn = waitingOn;
		this.videoRoot = videoRoot;
		this.baseurl = baseUrl;
		this.wait = b;
	}


	@Override
	public void run() {

		//wait for the dasher to conclude
		while(!waitingOn.isComplete() && wait){
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}


		logger.info("Dasher reports completed, creating accurate mpd file...");


		MPDtype mpd = null;
		try {
			mpd = this.getMPDFor(needMPD);

			List<BaseURLType> baseurls = mpd.getPeriod().get(0).getBaseURL();
			BaseURLType myBase = new BaseURLType();
			myBase.setValue(baseurl);
			baseurls.clear();
			baseurls.add(myBase);


		} catch (SAXException | IOException
				| ParserConfigurationException
				| DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Date now = new Date();
		long duration = (long) (((double)mpd.getMediaPresentationDuration().getTimeInMillis(now)) / (double)1000.00);
		logger.info("length in sec set to be: " + duration + " from XML duration: " + mpd.getMediaPresentationDuration());


		setComplete(true);

	}


	private MPDtype getMPDFor(Video toMake) throws SAXException, IOException, ParserConfigurationException, DatatypeConfigurationException{

		File manifest = new File(videoRoot, "Manifest.mpd");
		MPDtype generated = Marshal.parseMPD(manifest.toURI());
		
		return generated;


	}


	public boolean isComplete() {
		return complete;
	}


	private void setComplete(boolean complete) {
		this.complete = complete;
	}


	public Video getUpdatedMetadata() {
		return needMPD;
	}

}
