package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.geojson.Feature;
import org.geojson.Point;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;

import batch.CouchbaseConfig;

public class LocationAdder {
	
	final static ObjectMapper mapper = new ObjectMapper();
	
	static{
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	
	private static ArrayList<KnownLocation> scanForLocationFilesIn(File locationJsonDir) throws Exception {
		// set date created where needed and make sure ids are unique
		ArrayList<KnownLocation> locs = new ArrayList<KnownLocation>();
		HashMap<String, Long> ids = new HashMap<String, Long>();
		ids.put("example_id", 0L);

		String[] extensions = new String[]{"json"};
		Iterator<File> iter = FileUtils.iterateFiles(locationJsonDir, extensions, true);

		ObjectMapper mapper = new ObjectMapper();

		boolean changed = false; 
		while(iter.hasNext()){
			File toProcess= iter.next();
			if(toProcess.getName().contains("example")) {
				continue;
			}
			KnownLocation fileContents = null;
			try {
				fileContents = mapper.readValue(toProcess, KnownLocation.class);
			} catch (Exception e) {
				System.err.println("error reading in file: " + toProcess.getName());
				e.printStackTrace();
				continue;
			}

			//check id
			String id = fileContents.get_id();

			if(ids.keySet().contains(id) && !id.contains("example")){
				System.out.println("duplicate id found.");
				throw new Exception("DUPLICATE IDS: " + id);

			}

			ids.put(id, System.currentTimeMillis());
			locs.add(fileContents);
		}
		return locs; 
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
		
	
	public static void main(String[] args) throws Exception{
			Scanner scanner = new Scanner(System.in);
			boolean running = true;
			File operatingDir = new File(System.getProperty("user.dir"));
			File locationJsonDir = new File(operatingDir, "locations");
	

			if(!locationJsonDir.exists()){
				locationJsonDir.mkdirs();
			}
		while (running){
			ArrayList<KnownLocation> scannedLocations = scanForLocationFilesIn(locationJsonDir);
			
			System.out.print("include locations already in online db? ([y]/n): ");
			String include = scanner.nextLine();
			
			if(include.contains("n")){
				System.out.println("starting up db to make comparisons...");
				setupDB();
				
				for(Iterator<KnownLocation> iter = scannedLocations.iterator(); iter.hasNext();){
					KnownLocation cur = iter.next();
					if(bucket.exists(cur.get_id())){
						System.out.println("Location with id: " + cur.get_id() + " already exisits online. skipping");
						iter.remove();
					} else {
						System.out.println("Location with id: " + cur.get_id() + " does not exist. keeping.");
					}
				}
				
			}
			
			
			System.out.println("Locations available to add: ");

			for(int i = 0; i < scannedLocations.size(); i++){
				KnownLocation cur = scannedLocations.get(i);
				
				System.out.println(i + ": location Id: " + scannedLocations.get(i).get_id());
				System.out.println("      with name: " + cur.getName());
				System.out.println("      at location: " + cur.getLocation());
				System.out.println();
			}

			if(scannedLocations.size() == 0){
				System.out.println("None.");
			}
			System.out.println("\n");
			System.out.print("add which location #? (q to quit, e for example gen): ");
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
					generateExampleLocation(locationJsonDir);
				continue;

				default:
					System.out.println("Error - command not recognized: " + selection);
					Thread.sleep(150);
					continue;
				}
			}

			KnownLocation toAdd = scannedLocations.get(asNum);

			System.out.println("Adding location: " + toAdd.get_id());

		
			System.out.println();

		

			System.out.println("Beginning location online creation");
			KnownLocationCreatorThread creator = new KnownLocationCreatorThread(toAdd);
			creator.start();
			try {
				creator.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	
			KnownLocation added = creator.getAdded();
			
			if(added.get_id().equals(toAdd.get_id())){


			System.out.println("location created oinline successfully");
			} else {
				throw new Exception("Error - video not uploaded correctly"); 
			}
		}
		scanner.close();
	}

	private static void generateExampleLocation(File dir) throws Exception {
		KnownLocation maine = new KnownLocation();
		maine.setName("Example");
		//	        User uploader = app.getUserRepo().getLoggedInUser();
		Feature object = null;
		try {
			object=   new ObjectMapper().readValue("{\n" +
					"  \"type\": \"Feature\",\n" +
					"  \"properties\": {\n" +
					"    \"name\": \"North America\",\n" +
					"    \"area\": 60000000\n" +
					"  },\n" +
					"  \"geometry\": {\n" +
					"    \"type\": \"Polygon\",\n" +
					"    \"coordinates\": [\n" +
					"      [\n" +
					"        [-170.15625, 16.46769474828897],\n" +
					"        [-170.15625, 77.57995914400348],\n" +
					"        [ -54.4921875,  77.57995914400348],\n" +
					"        [ -54.4921875, 16.46769474828897]\n" +
					"      ]\n" +
					"    ]\n" +
					"  }\n" +
					"}   ", Feature.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		maine.setCreatorId("00000000000000000000000000000000000");

		maine.set_id("example");

		maine.setLocation(object);

		System.out.println("location: " + maine.getName() + " created successfully");
		File outputFile = new File(dir, "exampleLocation.json");
		if(outputFile.exists()){
			System.out.println("example already exists @ " + outputFile.getAbsolutePath());
			System.out.println("no changes made");
			return; 
		} else {
			outputFile.createNewFile();
		}
		
		FileWriter writer = new FileWriter(outputFile);

		String videoAsString = mapper.writeValueAsString(maine);

		writer.write(videoAsString);
		writer.flush();
		writer.close();
		System.out.print("File: " + outputFile.getAbsolutePath() + " written");
		return; 
	}

}
