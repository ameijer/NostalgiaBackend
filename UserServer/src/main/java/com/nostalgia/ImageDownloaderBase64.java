package com.nostalgia;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDownloaderBase64 implements Runnable{


	private static final String IDENTICON_IMAGE_FORMAT = "PNG";
	private final String targetImg;
	private static final Logger logger = LoggerFactory.getLogger(ImageDownloaderBase64.class);

	private String encodedImage; 
	
	public ImageDownloaderBase64(String targetImageURL){
		super();
		targetImg = targetImageURL;
	}
	
	@Override
	public void run() {
		BufferedImage image = null;
		byte[] imageBytes = null;
		try {
			URL url = new URL(targetImg);
			image = ImageIO.read(url);
		} catch (IOException e) {
			logger.error("error writing image from: " + targetImg, e);
		}

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

		try {
			ImageIO.write(image, IDENTICON_IMAGE_FORMAT, byteOut);
		} catch (IOException e) {
		logger.error("error writing image into byte[] png", e);
		}
		imageBytes = byteOut.toByteArray();
		
		encodedImage = Base64.getEncoder().encodeToString( imageBytes);
	}
	public String getEncodedImage() {
		return encodedImage;
	}
	
}