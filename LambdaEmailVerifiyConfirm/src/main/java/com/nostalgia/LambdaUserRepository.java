package com.nostalgia;

import java.util.ArrayList;
import java.util.Arrays;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.SpatialView;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.SpatialViewResult;
import com.couchbase.client.java.view.SpatialViewRow;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;

public class LambdaUserRepository {


	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();

	// Initialize design document
	DesignDocument userEmailDoc = DesignDocument.create(
			"user_email",
			Arrays.asList(
					DefaultView.create("by_token",
							"function (doc, meta) { if (doc.type == 'User') { emit(doc.settings.EMAIL_CODE, null); } }")
					)
			);
	public LambdaUserRepository(CouchbaseConfig lambdaCouchConfig) {
		config = lambdaCouchConfig;
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("user_email");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(userEmailDoc);
		}
	}

	public JsonDocument save(User adding) throws Exception {

		if(adding.get_id() == null) throw new Exception("non-null id required");

		String json = null;
		try {
			json = mapper.writeValueAsString(adding);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		JsonObject jsonObj = JsonObject.fromJson(json);

		if(jsonObj.get("_id") == null){
			jsonObj.put("_id", adding.get_id());
		}
		JsonDocument  doc = JsonDocument.create(adding.get_id(), jsonObj);

		if(doc.content().get("_id") == null){
			doc.content().put("_id", adding.get_id());
		}
		JsonDocument inserted = bucket.upsert(doc, PersistTo.MASTER);

		if(inserted.content().getString("_id") == null){
			throw new Exception("ID FIELD REQUIRED");
		}
		return inserted;  
	}

	public User findOneByEmailToken(String token) {
		ViewQuery query = ViewQuery.from("user_email", "by_token").inclusiveEnd(true).key(token);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			System.err.println("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		ArrayList<User> users = new ArrayList<User>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();
			System.out.println("found matching document: " + matching.id());
			users.add(docToUser(matching));
		}

		if(users.size() > 1){
			System.err.println("TOO MANY users MATCHING token: " + token);
		}
		if(users.size() < 1){
			System.out.println("no users found in repo findonebyemailtoen call");
			return null; 
		}
		return users.get(0);
	}

	public static User docToUser(JsonDocument document) {
		JsonObject obj = document.content();
		String objString = obj.toString();

		User newUser = null;
		try {
			newUser = mapper.readValue( objString , User.class );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return newUser; 
	}
}
