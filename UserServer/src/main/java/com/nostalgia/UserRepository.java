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
import com.nostalgia.persistence.model.User;

public class UserRepository {

	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();
	

	private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
	

	
	// Initialize design document
	DesignDocument userDoc = DesignDocument.create(
		"user",
		Arrays.asList(
			DefaultView.create("by_id",
				"function (doc, meta) { if (doc.type == 'User') { emit(doc._id, null); } }"),
			DefaultView.create("by_email",
				"function (doc, meta) { if (doc.type == 'User') { emit(doc.email, null); } }"),
			DefaultView.create("by_name",
				"function (doc, meta) { if (doc.type == 'User') { emit(doc.username, null); } }"),
			DefaultView.create("by_token",
					"function (doc, meta) { if (doc.type == 'User') { emit(doc.token, null); } }"),
			DefaultView.create("by_account_token",
					"function (doc, meta) { "
					+ "if (doc.type == 'User') { "
					+ "for (i=0; i < doc.accounts.length; i++) {"
					+ "emit(doc.accounts[i].value, doc.accounts[i].key); "
					+ "} "
					+ "} "
					+ "}")
		)
	);
	

	public UserRepository(CouchbaseConfig userCouchConfig) {
		config = userCouchConfig;
		cluster = CouchbaseCluster.create(userCouchConfig.host);
		bucket = cluster.openBucket(userCouchConfig.bucketName, userCouchConfig.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("user");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(userDoc);
		}

	}

	public synchronized JsonDocument save(User user) throws Exception {


		String json = null;
		try {
			json = mapper.writeValueAsString(user);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JsonObject jsonObj = JsonObject.fromJson(json);

		JsonDocument  doc = JsonDocument.create(user.get_id(), jsonObj);
		
		if(jsonObj.get("_id") == null){
			throw new Exception("error - _id field required ");
		}

		JsonDocument inserted = bucket.upsert(doc);
		
		if(!inserted.content().containsKey("_id")){
			logger.error("user object saved without id key!!", new Exception("caught user being added without key!"));
			System.exit(1);
		}
		return inserted;
	}

	public User docToUser(JsonDocument document) {
		if(document == null) return null;
		
		JsonObject obj = document.content();
		String objString = obj.toString();

		User newUser = null;
		try {
			newUser = mapper.readValue(objString, User.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(!newUser.get_id().equals(document.id())){
			logger.error("ERROR - USER ID FIELD DROPPED");
			logger.info("repairing...");
			document.content().put("_id", document.id());
			JsonDocument inserted = bucket.upsert(document);
			newUser.set_id(document.id());
		}
		return newUser; 
	}



	public User findOneById(String id) throws Exception {
		ViewQuery query = ViewQuery.from("user", "by_id").inclusiveEnd(true).key(id);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
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
			logger.error("TOO MANY USERS MATCHING id");
		}
		if(users.size() < 1) return null; 
		return users.get(0);

	}

	public List<User> findByName(String name) throws Exception {
		ViewQuery query = ViewQuery.from("user", "by_name").inclusiveEnd(true).key(name);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
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
			logger.error("TOO MANY USERS MATCHING NAME");
		}
		return users;

	}
	
	public User findOneByEmail(String email) throws Exception {
		ViewResult result = bucket.query(ViewQuery.from("user", "by_email").startKey(email).endKey(email).limit(10));

		if (result == null || result.totalRows() < 1){
			return null;
		}
		
		ArrayList<User> users = new ArrayList<User>();
		for (ViewRow row : result) {
		    JsonDocument matching = row.document();
		    
		    if(matching == null) continue;
		    users.add(docToUser(matching));
		}

		if(users.size() > 1){
			logger.error("TOO MANY USERS MATCHING EMAIL");
		}
		if(users.size() < 1){
			return null;
		}
		return users.get(0);

	}

	public User findOneByAccountToken(String userToken, String accountType) throws Exception {
		
		ViewResult result = bucket.query(ViewQuery.from("user", "by_account_token").key(userToken).limit(10));
		
		if (result == null || result.totalRows() < 1){
			return null;
		}
		
		ArrayList<User> users = new ArrayList<User>();
		for (ViewRow row : result) {
		    JsonDocument matching = row.document();
		    String val = row.value().toString();
		    
		    users.add(docToUser(matching));
		}
		
		if(users.size() > 1){
			logger.error("TOO MANY USERS MATCHING TOKEN");
		}
		return users.get(0);
	}

	public List<User> searchByName(String friendName) {
		ViewQuery query = ViewQuery.from("user", "by_name").startKey(friendName).endKey(friendName + "\uefff").limit(20);
		ViewResult result = bucket.query(query);
		if (result == null || result.totalRows() < 1){
			return null;
		}
		
		ArrayList<User> users = new ArrayList<User>();
		for (ViewRow row : result) {
		    JsonDocument matching = row.document();
		    
		    users.add(docToUser(matching));
		}
		
		
		return users;
	}

	public User findOneByOAuthToken(String access_token_id) {
		ViewQuery query = ViewQuery.from("user", "by_token").inclusiveEnd(true).key(access_token_id);//.stale(Stale.FALSE);
		ViewResult result = bucket.query(query/*.key(name).limit(10)*/);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
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
			logger.error("TOO MANY USERS MATCHING id");
		}
		if(users.size() < 1) return null; 
		return users.get(0);
	}

}
