package com.nostalgia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;


public class S3UlDlClient {

	private BasicAWSCredentials credentials;
	private TransferManager tx;
	private  String bucketName;

	private S3Config config;


	public S3UlDlClient(S3Config s3Config) throws Exception {
		super();
		

//		Properties properties = new Properties();
//		properties.load( getClass().getResourceAsStream("awscredentials") );
//
//		String accessKeyId = properties.getProperty( "accessKey" );
//		String secretKey = properties.getProperty( "secretKey" );
		credentials = new BasicAWSCredentials( s3Config.accessKeyId, s3Config.secretAccessKey);
		
		bucketName = s3Config.bucketName; 

		this.config = s3Config; 
		tx = new TransferManager(credentials);
		createAmazonS3Bucket();
	}

	private void createAmazonS3Bucket() {
		try {
			if (tx.getAmazonS3Client().doesBucketExist(bucketName) == false) {
				tx.getAmazonS3Client().createBucket(bucketName);
			}
		} catch (AmazonClientException ace) {
			System.err.println("error creating bucket\n" + ace);
		}
	}

	public File getDirFromPending(String dirName, File parentDirToSaveIn){
		File saved = new File(parentDirToSaveIn, dirName); 
		if(saved.exists()) return saved; 
		
		MultipleFileDownload myDownload = tx.downloadDirectory(bucketName, config.parentPendingFolder+ "/" + dirName , parentDirToSaveIn);

		
		try {
			myDownload.waitForCompletion();
		} catch (AmazonClientException | InterruptedException e) {
			System.err.println("error waiting for upload completion\n" + e);
			return null;

		}
		
		File rawSaved = new File(parentDirToSaveIn, "pending/" + dirName); 
		
		
		try {
			FileUtils.moveDirectory(rawSaved, saved);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(!saved.exists()){
			System.err.println("dir not saved in: " + saved.getAbsolutePath());
		}
		return saved; 	
	}
	
	public boolean deletePendingFile(String dirId){
		AmazonS3 s3Client = new AmazonS3Client(credentials);        
		if (s3Client.doesBucketExist(bucketName)) {
		    ObjectListing objects = s3Client.listObjects(bucketName, config.parentPendingFolder+ "/" + dirId);
		    for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
		        s3Client.deleteObject(bucketName, objectSummary.getKey());
		        System.out.println("deleting file: " + objectSummary.getKey());
		    }			
		} else return false;
		
		s3Client.deleteObject(new DeleteObjectRequest(bucketName, config.parentPendingFolder+ "/" + dirId));
		return true;

	}

	public boolean uploadDirToData(File tempDir) {
		MultipleFileUpload myUpload = tx.uploadDirectory(bucketName, "data/" + tempDir.getName() + "/" , tempDir, true);

		try {
			myUpload.waitForCompletion();
		} catch (AmazonClientException | InterruptedException e) {
			System.err.println("error waiting for upload completion"+ e);
			return false;

		}
		return true;	
		
	}

}
