package com.nostalgia.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.nostalgia.VideoRepository;
import com.nostalgia.client.LambdaClient;
import com.nostalgia.client.S3UploadClient;
import com.nostalgia.persistence.model.Video;
@Path("/api/v0/video/upload")
public class VideoUploadResource {

	@Context HttpServletResponse resp; 

	private static final Logger logger = LoggerFactory.getLogger(VideoUploadResource.class);

	private static final String FileDataWorkingDirectory = "data";
	private final VideoRepository vidRepo;

	private final S3UploadClient s3Cli;

	private final LambdaClient lCli;

	public VideoUploadResource(VideoRepository vidRepo, S3UploadClient s3Cli, LambdaClient lCli) {
		this.vidRepo = vidRepo;
		this.lCli = lCli; 
		this.s3Cli = s3Cli; 

	}
	//step 2 is to upload 
	@SuppressWarnings("unused")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes("*/*")
	@Path("/data")
	@Timed
	public String uploadVideoData(final InputStream fileInputStream,
			@Context HttpServletRequest a_request,
			@QueryParam("vidId") String contentKey,
			@QueryParam("checksum") String checksum) throws Exception{

		Video matching = null;
		int numTries = 0;

		while(matching == null && numTries < 10){
			numTries++;
			matching = vidRepo.findOneById(contentKey);
			Thread.sleep(150);
		}


		//Save video
		if(matching == null){
			throw new NotFoundException("null matching video!"); 
		}

		if(checksum == null || checksum.equalsIgnoreCase("")){
			throw new BadRequestException("checksum is required"); 
		}



		File dataDir = new File(FileDataWorkingDirectory + "/" + matching.get_id() );
		dataDir.mkdirs(); 



		String savedFilePath = dataDir + "/" + matching.get_id();
		File original = new File(savedFilePath);



		boolean isMatch = false;
		String savedmd5 = null;
		if(original.exists()){
			//check MD5
			savedmd5 = this.md5Of(original);
			//if they match, then skip the upload process
			if(savedmd5.equalsIgnoreCase(checksum)){
				isMatch = true;
				fileInputStream.close();
				logger.info("exact checksum match found, skipping upload");
			}
		} 

		if(!isMatch){
			//wipe out whatever was there
			if(original.exists()){
				FileUtils.forceDelete(original);
			}

			//re-upload it
			long start = System.currentTimeMillis();
			original = saveFile(fileInputStream, savedFilePath);
			long end = System.currentTimeMillis();

			Duration thisRun = Duration.ofMillis(end -start);
			long speed = - 1;
			try {
				speed = (original.length() * 8) / (((end - start) / 1000) * (long)Math.pow(2, 20));
			} catch (Exception e){
				logger.error("errror computing save time", e);
			}
			logger.info("File: " + original.getName() + " took " + thisRun.toString() + " to download at a speed of " + speed + "mbps");
		}




		if(original == null || !original.exists()){
			logger.error("no file saved!");
			return null;
		}


		//otherwise, assume file exists and is legit

		savedmd5 = md5Of(original);

		if(!savedmd5.equalsIgnoreCase(checksum)){
			//MD5 failure
			logger.error("file upload failed MD5 verification");
			FileUtils.forceDelete(original);
			return null; 
		}


		boolean success = s3Cli.uploadDirToS3(dataDir);

		if(success){
			logger.info("file upload successful. deleting source dir: " + dataDir);
			FileUtils.deleteDirectory(dataDir);
		}

		matching.setStatus("PENDING_PROCESSING");

		String source = matching.getProperties().get("vidsource");
		final boolean isNative = source != null && source.equalsIgnoreCase("NATIVE");

		if(source == null){
			matching.getProperties().put("vidsource", "NOT_SPECIFIED"); 
		}
		logger.info("firing transcode request off to content processor");
		final Video toProcess = matching; 
		Thread processStarter = new Thread(){


			@Override
			public void run(){
				try {
					lCli.processVideo(toProcess.get_id(), isNative);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};


		processStarter.start();
		vidRepo.save(matching);
		//start the transcoding process
		

		
		
		return savedmd5;

	}


	private synchronized static String md5Of(File saved) throws IOException{
		//verify with MD5


		FileInputStream fis = new FileInputStream(saved);
		String savedmd5 = new String(DigestUtils.md5Hex(fis));
		fis.close();

		return savedmd5;

	}



	private File saveFile(InputStream uploadedInputStream,
			String serverLocation) {
		File target = new File(serverLocation);
		try {
			OutputStream outputStream = new FileOutputStream(new File(serverLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = uploadedInputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			logger.error("error saving uploaded file to disk", e);
			return null;
		}

		return target;
	} 
}
