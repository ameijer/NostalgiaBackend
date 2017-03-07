package com.nostalgia.persistence.model.icon;

import java.io.Serializable;

public class IconReply implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 165433653564346L;
	
	private byte[] encodedImage;
	
	private boolean usingDefault = false;

	public byte[] getEncodedImage() {
		return encodedImage;
	}

	public void setEncodedImage(byte[] encodedImage) {
		this.encodedImage = encodedImage;
	}

	public boolean isUsingDefault() {
		return usingDefault;
	}

	public void setUsingDefault(boolean usingDefault) {
		this.usingDefault = usingDefault;
	}
	
	
}
