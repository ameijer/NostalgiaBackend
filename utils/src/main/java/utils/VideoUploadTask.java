package utils; 

/**
 * Created by alex on 10/30/15.
 */

/**
 * Created by alex on 8/30/15.
 */


import com.fasterxml.jackson.databind.ObjectMapper;

import com.nostalgia.persistence.model.Video;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class VideoUploadTask extends Thread {

	private static final String server = "REDACTED";
	private static final   int port = 10004;
	private static final String UPLOAD_URL_METADATA= server + ":" + port+ "/api/v0/video/new";
	private static final String UPLOAD_URL_VIDDATA= server + ":" + port + "/api/v0/video/data";
	private final boolean autoAdd;

	private static final ObjectMapper mapper = new ObjectMapper();
	private final List<String> tags;

	private int uploadFileForVid(String vidId, String MD5, File toUpload) throws Exception{
		String charset = "UTF-8";
		HttpPost httppost= new HttpPost(UPLOAD_URL_VIDDATA + "?vidId=" + vidId + "&checksum=" + MD5);

		boolean exists = toUpload.exists();
		HttpClient a_client = new DefaultHttpClient();
		InputStreamEntity reqEntity = new InputStreamEntity(
				new FileInputStream(toUpload), -1);
		reqEntity.setContentType("binary/octet-stream");
		reqEntity.setChunked(true); // Send in multiple parts if needed
		httppost.setEntity(reqEntity);
		HttpResponse response = a_client.execute(httppost);
		return response.getStatusLine().getStatusCode();
	}

	private String uploadVidMetadata(Video metadata,List<String> tagIds) throws Exception{
		String charset = "UTF-8";
		HttpPost httppost= new HttpPost(UPLOAD_URL_METADATA + "?auto=" + Boolean.toString(autoAdd) + "&idTags=" +mapper.writeValueAsString(tagIds));

		HttpClient a_client = new DefaultHttpClient();
		ObjectMapper om = new ObjectMapper();

		String videoAsJSON = om.writeValueAsString(metadata);
		StringEntity se = new StringEntity(videoAsJSON);
		//se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		httppost.setEntity(se); // Send in multiple parts if needed
		httppost.setHeader("Accept", "application/json");
		httppost.setHeader("Content-type", "application/json");
		HttpResponse response = a_client.execute(httppost);
		String uploaded = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		return uploaded;
	}


	public void setFinishedListener(UploadTaskFinishedListener finishedListener) {
		this.finishedListener = finishedListener;
	}

	public interface UploadTaskFinishedListener {
		void onTaskFinished(); // If you want to pass something back to the listener add a param to this method
	}

	public static final String TAG = "LoadingTask";
	// This is the progress bar you want to update while the task is in progress
	private UploadTaskFinishedListener finishedListener;




	private final Video toUpload;
	private final String targetPath;




	public VideoUploadTask(String focusedFilePath, Video thisVideo, List<String> taggedIds, boolean autoAddLocations) {

		this.toUpload = thisVideo;
		this.targetPath = focusedFilePath;
		this.autoAdd = autoAddLocations;
		this.tags = taggedIds;
	}

	@Override
	public void run(){
		int resultCode = -1;

		////upload metadata
		String savedId = null;
		try {
			savedId = uploadVidMetadata(toUpload, tags);
		} catch (Exception e) {
			e.printStackTrace();
		}

		File in = new File(targetPath);

		FileInputStream fis = null;
		String md5 = null;
		try {
			fis = new FileInputStream(in);
			long size = fis.getChannel().size();
			md5 = new String(Hex.encodeHex(DigestUtils.md5(fis)));
			fis.close();
		} catch (Exception e) {
			System.err.println(TAG + "error generating md5" +e);
		}
		//using returned data, upload video
		try {

			if (!in.exists()) {
				throw new Exception("file not found");
			}
			resultCode = uploadFileForVid(savedId, md5, in);

			// sendFileToServer(savedId, md5, in);
		} catch (Exception e) {
			System.err.println(TAG + "error uploading video" + e);
		}

	}

}
