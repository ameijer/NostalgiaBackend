package utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geojson.Feature;
import org.geojson.Point;
import org.json.JSONArray;
import org.json.JSONObject;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.LoginResponse;
import com.nostalgia.persistence.model.MediaCollection;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;

import batch.CouchbaseConfig;

public class UserAdder {



	public static void main(String[] args) throws Exception{
		Scanner scanner = new Scanner(System.in);
		boolean running = true;
		File operatingDir = new File(System.getProperty("user.dir"));
		File userJsonDir = new File(operatingDir, "users");
		File userPicDir = new File(operatingDir, "userpics");

		if(!userJsonDir.exists()){
			userJsonDir.mkdirs();
		}
		if(!userPicDir.exists()){
			userPicDir.mkdirs();
		}


		System.out.println("Welcome to the user adder. Scanning for .json files in: " + userJsonDir.getAbsolutePath() + "...");
		while (running){


			ArrayList<User> scannedUsers = scanForUserFilesIn(userJsonDir);

			if(scannedUsers.size() > 0){
				System.out.print("Include users that already exist in the online db? ([y]/n):");
				String ans = scanner.nextLine(); 

				if(ans == null || ans.equals("") || ans.length() == 0 || !ans.equals("n")){
					//default to yes
					System.out.println("including all scanned users...");
				} else {
					System.out.println("excluding online users...");
					//search db and chop out any users that already exist online
					System.out.println("initing db...");
					setupDB();
					System.out.println("db inited");

					for(Iterator<User> iter = scannedUsers.iterator(); iter.hasNext();){
						User scanned = iter.next(); 
						System.out.println("Checking for existence of user: " + scanned.getUsername());
						if(bucket.get(scanned.get_id()) != null){
							System.out.println("User: " + scanned.getUsername() + " exists, not re-adding...");
							iter.remove();
						}
					}

				}


				System.out.println("Users found: ");

				for(int i = 0; i < scannedUsers.size(); i++){
					User cur = scannedUsers.get(i);

					System.out.println(i + ": USer name: " + scannedUsers.get(i).getUsername());
					System.out.println("      with id: " + scannedUsers.get(i).get_id());
					System.out.println("      created on: " + scannedUsers.get(i).getDateJoined());
					System.out.println();
				}
			}

			if(scannedUsers.size() == 0){
				System.out.println("None.");
			}
			System.out.println("\n");
			System.out.print("add which user #? (q to quit, e for example gen): ");
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
					generateExampleJsonForUser(userJsonDir);
				continue;

				default:
					System.out.println("Error - command not recognized: " + selection);
					Thread.sleep(150);
					continue;
				}
			}

			User toAdd = scannedUsers.get(asNum);

			System.out.println("Adding user: " + toAdd.getUsername());

			if(toAdd.getIcon() == null || toAdd.getIcon().length() < 10){
				toAdd.setIcon(null);
				System.out.println("no encoded icon found in json for: " + toAdd.getUsername());
				System.out.print("add local icon to user? (y/[n])(answering no will cause a default icon to be generated server side): ");

				String answer = scanner.nextLine();
				if(answer == null || answer.equals("") || answer.contains("n")){
					System.out.println("skipping photo add.");

				} else {

					System.out.println("Enter the path to the users's icon, or [ENTER] to search in: " + userPicDir.getAbsolutePath());
					System.out.println("Note - the data will be copied into the data dir if alternate path specified");

					String path = scanner.nextLine();

					String searchPath = null;

					if(path.equals("")){
						System.out.println("Searching in: " + userPicDir.getAbsolutePath() + " for file: " + toAdd.get_id());
						searchPath = userPicDir.getAbsolutePath() + "/" + toAdd.get_id() + ".png";

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

					File saved = new File(userPicDir, toAdd.get_id() + ".png");
					if(!data.getAbsolutePath().contains(userPicDir.getName())){
						//copy
						System.out.println("Copying file...");
						FileUtils.copyFile(data, saved);





					}
					FileInputStream fileBytes = new FileInputStream(saved); 
					byte[] defaultEncoded = IOUtils.toByteArray(fileBytes);
					System.out.println("encoding to base64 and attaching to user");
					toAdd.setIcon(new String(Base64.encodeBase64(defaultEncoded, false, false)));
					System.out.println("done");
				}
			}

			System.out.print("add user created videos(y/n)? " );
			String tagAns = scanner.nextLine();

			if(tagAns.contains("y") || tagAns.contains("Y")){
				addUserCreatedVideos(toAdd);
			} else {
				System.out.println("Not adding any user created videos");
			}
			System.out.println();

			System.out.print("add user collections (y/n)? " );
			tagAns = scanner.nextLine();

			if(tagAns.contains("y") || tagAns.contains("Y")){
				addUserColls(toAdd);
			} else {
				System.out.println("Not adding any user colls");
			}
			System.out.println();

			FileWriter writer = new FileWriter(new File(userJsonDir, toAdd.get_id() + ".json"));

			String videoAsString = mapper.writeValueAsString(toAdd);

			writer.write(videoAsString);
			writer.flush();
			writer.close();


			System.out.println("Beginning user registration");
			System.out.print("enter password: " );
			String pass = scanner.nextLine();
			LoginRegisterThread task = new LoginRegisterThread(toAdd, pass, true, "app");
			task.start();
			task.join();

			LoginResponse resp = task.getLoginResponse();

			if(resp.getSessionTok() != null){
				System.out.println("user uploaded successfully");
			} else {
				System.err.println("error registering user");
			}
		}
		scanner.close();
	}

	private static void addUserColls(User toAdd) throws Exception {
		setupDB();
		for(String key:toAdd.getCollections().keySet()){
			if(key.contains(toAdd.get_id())){
				continue; 
			}
			JSONArray obj = new JSONArray(key);
			System.out.println("user collection: " + obj);
			JSONObject vis = obj.getJSONObject(0);
			JSONObject name = obj.getJSONObject(1);
			String id = toAdd.getCollections().get(key);

			MediaCollection coll = new MediaCollection();
			coll.set_id(id);
			coll.setName(name.getString("key"));
			coll.setVisibility(vis.getString("visibility"));

			String json = null;
			try {
				json = mapper.writeValueAsString(coll);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JsonObject jsonObj = JsonObject.fromJson(json);
			JsonDocument  doc = JsonDocument.create(coll.get_id(), jsonObj);

			if(jsonObj.get("_id") == null){
				throw new Exception("error - _id field required ");
			}

			JsonDocument inserted = bucket.upsert(doc);
			System.out.println("inserted collection with id: " + inserted.id() + " and key: " + key);

		}

	}

	private static void addUserCreatedVideos(User toAdd) {
		// TODO Auto-generated method stub

	}

	private static void removeUserFrom(ArrayList<User> scannedUsers, User scanned) {
		for(int i = 0; i < scannedUsers.size(); i++){
			if(scannedUsers.get(i).get_id().equals(scanned.get_id())){
				//this is the one to remove
				scannedUsers.remove(i);
				return; 
			}
		}

		System.err.println("no matching user found to remove");

	}

	// the DB we are using
	private static Cluster cluster; 
	private static  Bucket bucket; 
	private static  CouchbaseConfig config ;
	private static BucketManager bucketManager;

	private static void setupDB() {
		if(bucket == null){
			config = new CouchbaseConfig();
			cluster = CouchbaseCluster.create(config.host);
			bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
			bucketManager = bucket.bucketManager();
		}
	}

	final static ObjectMapper mapper = new ObjectMapper();
	static{
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	private static void generateExampleJsonForUser(File userJsonDir) throws IOException {
		File outputFile = new File(userJsonDir, "exampleUser.json");
		userJsonDir.mkdirs();
		if(outputFile.exists()){
			System.out.println("example already exists @ " + outputFile.getAbsolutePath());
			System.out.println("no changes made");
			return; 
		} else {
			outputFile.createNewFile();
		}

		User example = new User();
		example.set_id("example_id");
		example.setUsername("Example User");
		example.setEmail("example@example.com");
		example.setSettings(new HashMap<String, String>());
		example.getSettings().put("sync", "always");
		example.getSettings().put("sharing_who", "EVERYONE");
		example.getSettings().put("sharing_when", "WIFI");
		example.getSettings().put("sharing_where", "EVERYWHERE");
		example.getSettings().put("video_sound", "MUTE");
		FileWriter writer = new FileWriter(outputFile);

		String videoAsString = mapper.writeValueAsString(example);

		writer.write(videoAsString);
		writer.flush();
		writer.close();

		return; 
	}

	private static ArrayList<User> scanForUserFilesIn(File userJsonDir) throws Exception {
		// set date created where needed and make sure ids are unique
		ArrayList<User> users = new ArrayList<User>();
		HashMap<String, Long> ids = new HashMap<String, Long>();
		ids.put("example_id", 0L);

		String[] extensions = new String[]{"json"};
		Iterator<File> iter = FileUtils.iterateFiles(userJsonDir, extensions, true);

		ObjectMapper mapper = new ObjectMapper();

		boolean changed = false; 
		while(iter.hasNext()){
			File toProcess= iter.next();
			if(toProcess.getName().equals("exampleUser.json")) {
				continue;
			}
			User fileContents = null;
			try {
				fileContents = mapper.readValue(toProcess, User.class);
			} catch (Exception e) {
				System.err.println("error reading in file: " + toProcess.getName());
				e.printStackTrace();
				continue;
			}

			//set date created if necessary 
			if(fileContents.getDateJoined()< 1000000){
				System.out.println("No date joined found. creating one...");
				changed = true;
				fileContents.setDateJoined(System.currentTimeMillis());

			}

			//check id
			String id = fileContents.get_id();

			if(ids.keySet().contains(id) && !id.contains("example")){
				System.out.println("duplicate id found.");
				throw new Exception("DUPLICATE IDS: " + id);

			}

			ids.put(id, System.currentTimeMillis());
			users.add(fileContents);
		}
		return users; 
	}

}
