package com.nostalgia.client;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.nostalgia.S3Config;

import io.dropwizard.lifecycle.Managed;


public class S3UploadClient implements Managed {

	final static Logger logger = LoggerFactory.getLogger(S3UploadClient.class);

	private BasicAWSCredentials credentials;
	private TransferManager tx;
	private  String bucketName;

	private S3Config config;


	public S3UploadClient(S3Config s3Config) throws Exception {
		super();
		
		credentials = new BasicAWSCredentials(s3Config.accessKeyId, s3Config.secretAccessKey);
		
		bucketName = s3Config.bucketName; 

		this.config = s3Config; 
		
		
	}

	private void createAmazonS3Bucket() {
		try {
			if (tx.getAmazonS3Client().doesBucketExist(bucketName) == false) {
				tx.getAmazonS3Client().createBucket(bucketName);
			}
		} catch (AmazonClientException ace) {
			logger.error("error creating bucket", ace);
		}
	}

	@Override
	public void start() throws Exception {
		tx = new TransferManager(credentials);
		createAmazonS3Bucket();
	}

	@Override
	public void stop() throws Exception {
		tx.shutdownNow();
	}

	public synchronized boolean uploadDirToS3(File dirToUpload){

		MultipleFileUpload myUpload = tx.uploadDirectory(bucketName, config.parentUploadFolder + "/" + dirToUpload.getName() + "/" , dirToUpload, true);

		try {
			myUpload.waitForCompletion();
		} catch (AmazonClientException | InterruptedException e) {
			logger.error("error waiting for upload completion", e);
			return false;

		}
		return true;	
	}

}
