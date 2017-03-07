package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.geojson.Feature;
import org.geojson.Point;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;

import batch.CouchbaseConfig;

public class VideoAdder {



	public static void main(String[] args) throws Exception{
		Scanner scanner = new Scanner(System.in);
		boolean running = true;
		File operatingDir = new File(System.getProperty("user.dir"));
		File videoJsonDir = new File(operatingDir, "videos");
		File videoDataDir = new File(operatingDir, "videodata");

		if(!videoJsonDir.exists()){
			videoJsonDir.mkdirs();
		}
		System.out.println("Welcome to the video adder. Scanning for .json files in: " + videoJsonDir.getAbsolutePath() + "...");
		while (running){


			ArrayList<Video> scannedVideos = scanForVideoFilesIn(videoJsonDir);
			System.out.println("Videos found: ");

			for(int i = 0; i < scannedVideos.size(); i++){
				Video cur = scannedVideos.get(i);
				String comment = "none";
				if(cur.getProperties() != null){
					for(String key : cur.getProperties().keySet()){
						if(key.contains("comment") || key.contains("thought")){
							comment = cur.getProperties().get(key);
						}
					}
				}
				System.out.println(i + ": Video Id: " + scannedVideos.get(i).get_id());
				System.out.println("      with comment: " + comment);
				System.out.println("      created on: " + cur.getDateCreated());
				System.out.println("      at location: " + cur.getLocation());
				System.out.println();
			}

			if(scannedVideos.size() == 0){
				System.out.println("None.");
			}
			System.out.println("\n");
			System.out.print("add which video #? (q to quit, e for example gen): ");
			String selection = null;

			selection = scanner.nextLine();
			int asNum = -1;
			try {
				asNum = Integer.parseInt(selection);
			} catch (Exception e){
				switch(selection){

				case("q"):
					System.out.println("Goodbye");
				System.exit(0);
				break;

				case("e"):
					generateExampleJsonForVideo(videoJsonDir);
				continue;

				default:
					System.out.println("Error - command not recognized: " + selection);
					Thread.sleep(150);
					continue;
				}
			}

			Video toAdd = scannedVideos.get(asNum);

			System.out.println("Adding video: " + toAdd.get_id());

			System.out.println("Enter the path to the video's data, or [ENTER] to search in: " + videoDataDir.getAbsolutePath());
			System.out.println("Note - the data will be copied into the data dir if alternate path specified");

			String path = scanner.nextLine();

			String searchPath = null;

			if(path.length() < 2){
				System.out.println("Searching in: " + videoDataDir.getAbsolutePath() + " for file: " + toAdd.get_id());
				searchPath = videoDataDir.getAbsolutePath() + "/" + toAdd.get_id();

			} else {
				System.out.println("Searching in: " + path);
				searchPath = path;
			}

			File data = new File(searchPath);
			if(!data.exists()){
				System.err.println("Error - no file found at: " + path);
				continue;
			}

			//we have a data file we know exists 

			System.out.println("File found @" + data.getAbsolutePath());

			File saved = new File(videoDataDir, toAdd.get_id());
			if(!data.getAbsolutePath().contains(videoDataDir.getName())){
				//copy
				System.out.println("Copying file...");
				FileUtils.copyFile(data, saved);
				System.out.println("done");
			}

			System.out.print("tag video with locations(y/n)? " );
			String tagAns = scanner.nextLine();

			if(tagAns.contains("y") || tagAns.contains("Y")){
			} else {
				System.out.println("Not tagging with any locations");
			}
			System.out.println();

			FileWriter writer = new FileWriter(new File(videoJsonDir, toAdd.get_id() + ".json"));

			String videoAsString = mapper.writeValueAsString(toAdd);

			writer.write(videoAsString);
			writer.flush();
			writer.close();


			System.out.println("Beginning video upload");
			VideoUploadTask task = new VideoUploadTask(saved.getAbsolutePath(), toAdd, null, true);
			task.start();
			task.join();

			System.out.println("video uploaded successfully");

		}
		scanner.close();
	}

	// the DB we are using
	private static Cluster cluster; 
	private static  Bucket bucket; 
	private static  CouchbaseConfig config ;
	private static BucketManager bucketManager;

	private static void setupDB() {
		config = new CouchbaseConfig();
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();

	}
	
	final static ObjectMapper mapper = new ObjectMapper();
	static{
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	private static void generateExampleJsonForVideo(File videoJsonDir) throws IOException {
		File outputFile = new File(videoJsonDir, "exampleVideo.json");
		videoJsonDir.mkdirs();
		if(outputFile.exists()){
			System.out.println("example already exists @ " + outputFile.getAbsolutePath());
			System.out.println("no changes made");
			return; 
		} else {
			outputFile.createNewFile();
		}

		Video example = new Video();
		example.set_id("example_id");
		Point examplePoint = new Point(35.9999999, -79.0096901);
		example.setLocation(examplePoint);
		example.setUrl("<filled serverside>");
		example.setOwnerId("<insert owner id here>");
		example.setProperties(new HashMap<String, String>());
		example.getProperties().put("comment", "example comment");
		example.getProperties().put("sharing_who", "EVERYONE");
		example.getProperties().put("sharing_when", "WIFI");
		example.getProperties().put("sharing_where", "EVERYWHERE");
		example.getProperties().put("video_sound", "MUTE");

		FileWriter writer = new FileWriter(outputFile);

		String videoAsString = mapper.writeValueAsString(example);

		writer.write(videoAsString);
		writer.flush();
		writer.close();

		return; 
	}

	private static ArrayList<Video> scanForVideoFilesIn(File videoJsonDir) throws Exception {
		// set date created where needed and make sure ids are unique
		ArrayList<Video> vids = new ArrayList<Video>();
		HashMap<String, Long> ids = new HashMap<String, Long>();
		ids.put("example_id", 0L);

		String[] extensions = new String[]{"json"};
		Iterator<File> iter = FileUtils.iterateFiles(videoJsonDir, extensions, true);

		ObjectMapper mapper = new ObjectMapper();

		boolean changed = false; 
		while(iter.hasNext()){
			File toProcess= iter.next();
			if(toProcess.getName().equals("exampleVideo.json")) {
				continue;
			}
			Video fileContents = null;
			try {
				fileContents = mapper.readValue(toProcess, Video.class);
			} catch (Exception e) {
				System.err.println("error reading in file: " + toProcess.getName());
				e.printStackTrace();
				continue;
			}

			//set date created if necessary 
			if(fileContents.getDateCreated() < 1000000){
				System.out.println("No date found. creating one...");
				changed = true;
				fileContents.setDateCreated(System.currentTimeMillis());

			}

			//check id
			String id = fileContents.get_id();

			if(ids.keySet().contains(id) && !id.contains("example")){
				System.out.println("duplicate id found.");
				throw new Exception("DUPLICATE IDS: " + id);

			}

			ids.put(id, System.currentTimeMillis());
			vids.add(fileContents);
		}
		return vids; 
	}

}
