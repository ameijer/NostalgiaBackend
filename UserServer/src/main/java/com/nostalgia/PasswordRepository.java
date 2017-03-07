package com.nostalgia;
import flexjson.JSONDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.SpatialView;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.Password;
import com.nostalgia.persistence.model.User;
import com.nostalgia.util.PasswordUtils;

public class PasswordRepository {

	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();


	private static final Logger logger = LoggerFactory.getLogger(PasswordRepository.class);



	// Initialize design document
	DesignDocument passwordDoc = DesignDocument.create(
			"password",
			Arrays.asList(
					DefaultView.create("by_id",
							"function (doc, meta) { emit(doc._id, null); }"),
					DefaultView.create("by_owner_id",
							"function (doc, meta) { emit(doc.ownerId, null); }")
					)
			);


	public PasswordRepository(CouchbaseConfig passwordCouchConfig) {
		config = passwordCouchConfig;
		cluster = CouchbaseCluster.create(passwordCouchConfig.host);
		bucket = cluster.openBucket(passwordCouchConfig.bucketName, passwordCouchConfig.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("password");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(passwordDoc);
		}

	}

	public synchronized JsonDocument save(Password pass) throws Exception {


		String json = null;
		try {
			json = mapper.writeValueAsString(pass);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JsonObject jsonObj = JsonObject.fromJson(json);

		JsonDocument  doc = JsonDocument.create(pass.get_id(), jsonObj);

		if(jsonObj.get("_id") == null){
			throw new Exception("error - _id field required ");
		}

		JsonDocument inserted = bucket.upsert(doc);
		return inserted;
	}

	public Password docToPassword(JsonDocument document) {
		if(document == null) return null;

		JsonObject obj = document.content();
		String objString = obj.toString();

		Password newPass = null;
		try {
			newPass = mapper.readValue(objString, Password.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(!newPass.get_id().equals(document.id())){
			logger.error("ERROR - PASS ID FIELD DROPPED");
			logger.info("repairing...");
			document.content().put("_id", document.id());
			JsonDocument inserted = bucket.upsert(document);
			newPass.set_id(document.id());
		}
		return newPass; 
	}



	public Password findOneById(String id) throws Exception {
		ViewQuery query = ViewQuery.from("password", "by_id").inclusiveEnd(true).key(id);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		ArrayList<Password> passes = new ArrayList<Password>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			passes.add(docToPassword(matching));
		}

		if(passes.size() > 1){
			logger.error("TOO MANY passwords MATCHING id");
		}
		if(passes.size() < 1) return null; 
		return passes.get(0);

	}

	public Password findOneByOwnerId(String id) throws Exception {
		ViewQuery query = ViewQuery.from("password", "by_owner_id").inclusiveEnd(true).key(id);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		ArrayList<Password> passes = new ArrayList<Password>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			passes.add(docToPassword(matching));
		}

		if(passes.size() > 1){
			logger.error("TOO MANY passwords MATCHING id: " + id);
		}
		if(passes.size() < 1) return null; 
		return passes.get(0);

	}

	public boolean checkPassword(User user, String password) throws Exception {
		
		//check for up to 5 seconds
		int tries = 0;
		int maxTries = 10;
		Password matching = null;
		while(tries < maxTries){
			matching = findOneByOwnerId(user.get_id());
			
			if(matching == null){
				Thread.sleep(500);
			} else break;
			tries++;
		}
		

		User result = null; 
		
		if(matching == null){
			return false;
		}

		switch(matching.getVersion()){
		case 2:{
			//salted
			if(PasswordUtils.check(password, matching.getPassword())){
				result = user;
				break;
			}
		}
		break; 

		default:
			if(matching.getPassword().equalsIgnoreCase(password)){
				//then we have a match
				result = user; 
				break;
			}
			break;
		}



		return result != null; 

	}
}
