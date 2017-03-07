package com.nostalgia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geojson.GeoJsonObject;
import org.geojson.Polygon;

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

	public LambdaUserRepository(CouchbaseConfig lambdaCouchConfig) {
		config = lambdaCouchConfig;
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();

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

	public User findOneById(String id) {
		ViewQuery query = ViewQuery.from("user", "by_id").inclusiveEnd(true).key(id);
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
		    
		    users.add(docToUser(matching));
		}

		if(users.size() > 1){
			System.err.println("TOO MANY users MATCHING id");
		}
		if(users.size() < 1) return null; 
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
