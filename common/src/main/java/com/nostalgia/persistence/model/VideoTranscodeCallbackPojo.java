package com.nostalgia.persistence.model;

import java.io.Serializable;
import java.util.List;

public class VideoTranscodeCallbackPojo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7048927429713139126L;
	
	
	public List<String> thumbSuffix; 
	
	public String urlSuffix; 
	
	public String targetId; 

}
