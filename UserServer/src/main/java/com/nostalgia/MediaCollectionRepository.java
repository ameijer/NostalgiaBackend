package com.nostalgia;

import java.util.*;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.GeometryCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.MediaCollection;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;
import com.couchbase.client.java.view.*;
import flexjson.JSONDeserializer;

public class MediaCollectionRepository {

	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();


	private static final Logger logger = LoggerFactory.getLogger(MediaCollectionRepository.class);

	public MediaCollection findOneById(String id) throws Exception {
		ViewQuery query = ViewQuery.from("mediaCollection_standard", "by_id").inclusiveEnd(true).key(id);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}
	

		if (result == null || result.totalRows() < 1){
			return null;
		}
		
		ArrayList<MediaCollection> colls = new ArrayList<MediaCollection>();
		for (ViewRow row : result) {
		    JsonDocument matching = row.document();
		    
		    colls.add(docToCollection(matching));
		}

		if(colls.size() > 1){
			logger.error("TOO MANY colls MATCHING ID");
		}
		if(colls.size() < 1) return null; 
		return colls.get(0);

	}

	// Initialize design document
	DesignDocument collDoc = DesignDocument.create(
			"mediaCollection_standard",
			Arrays.asList(
					DefaultView.create("by_name",
							"function (doc, meta) { if (doc.type == 'MediaCollection') { emit(doc.name, null); } }"),
					DefaultView.create("by_id",
							"function (doc, meta) { if (doc.type == 'MediaCollection') { emit(doc._id, null); } }"),
					DefaultView.create("by_channel",
							"function (doc, meta) { "
									+ "if (doc.type == 'MediaCollection') { "
									+ "for (i=0; i < doc.channels.length; i++) {"
									+ "emit(doc.channels[i], null); "
									+ "} "
									+ "} "
									+ "}"),
					DefaultView.create("by_tag",
											"function (doc, meta) { "
													+ "if (doc.type == 'MediaCollection') { "
													+ "for (i=0; i < doc.tags.length; i++) {"
													+ "emit(doc.tags[i], null); "
													+ "} "
													+ "} "
													+ "}")

					)
			);




	public MediaCollectionRepository(CouchbaseConfig collectionCouchConfig) {
		config = collectionCouchConfig;
		cluster = CouchbaseCluster.create(collectionCouchConfig.host);
		bucket = cluster.openBucket(collectionCouchConfig.bucketName, collectionCouchConfig.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("mediaCollection_standard");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(collDoc);
		}
	}

	public List<MediaCollection> findByName(String name) throws Exception {
		ViewQuery query = ViewQuery.from("mediaCollection_standard", "by_name").inclusiveEnd(true).key(name);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		ArrayList<MediaCollection> colls = new ArrayList<MediaCollection>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			colls.add(docToCollection(matching));
		}
		return colls;
	}

	public static MediaCollection docToCollection(JsonDocument document) {
		JsonObject obj = document.content();
		String objString = obj.toString();

		MediaCollection coll = null;
		try {
			coll = mapper.readValue( objString , MediaCollection.class );
		} catch (Exception e) {
			logger.error("error converting mediacollection to doc", e);
		}

		return coll; 
	}

	public HashSet<MediaCollection> findByChannel(String channel) {
		HashSet<MediaCollection> s = (HashSet<MediaCollection>) Collections.synchronizedSet(new HashSet<MediaCollection>());

		ViewQuery query = ViewQuery.from("mediaCollection_standard", "by_channel").inclusiveEnd(true).key(channel);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}
		if (result == null || result.totalRows() < 1){
			return null;
		}

		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			s.add(docToCollection(matching));
		}

		return s;
	}
	
	public HashSet<MediaCollection> findByTag(String tag) {
		HashSet<MediaCollection> s = (HashSet<MediaCollection>) Collections.synchronizedSet(new HashSet<MediaCollection>());

		ViewQuery query = ViewQuery.from("mediaCollection_standard", "by_tag").inclusiveEnd(true).key(tag);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}
		if (result == null || result.totalRows() < 1){
			return null;
		}

		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			s.add(docToCollection(matching));
		}

		return s;
	}

	public synchronized JsonDocument save(MediaCollection coll) {
		//fix channel name
		coll.getChannels().clear();
		coll.getChannels().add(coll.getChannelName());


		String json = null;

		try {
			json = mapper.writeValueAsString(coll);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JsonObject jsonObj = JsonObject.fromJson(json);

		JsonDocument  doc = JsonDocument.create(coll.get_id(), jsonObj);

		JsonDocument inserted = bucket.upsert(doc);
		return inserted;
	}
	
	public JsonDocument remove(MediaCollection toDelete) {
		// Remove the document and make sure the delete is persisted.
		JsonDocument doc = bucket.remove(toDelete.get_id());
		return doc;
	}


	


}
