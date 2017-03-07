package com.nostalgia.persistence.model;

import java.io.Serializable;

public class ProcessContentRequestPojo implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2854092926965476214L;
	
	public static final String NATIVE = "NATIVE";
	public static final String STANDARD = "STANDARD";
	
	
	public String id; 
	public String mode = STANDARD; 

}
