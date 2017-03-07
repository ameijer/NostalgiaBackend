package com.nostalgia.contentserver.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.geojson.GeoJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
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
import com.nostalgia.contentserver.CouchbaseConfig;
import com.nostalgia.persistence.model.Video;

public class VideoRepository {

	private static final Logger logger = LoggerFactory.getLogger(VideoRepository.class);

	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();

	// Initialize design document
	DesignDocument vidDoc = DesignDocument.create(
			"video_processor_standard",
			Arrays.asList(
					DefaultView.create("by_name",
							"function (doc, meta) { if (doc.type == 'Video') { emit(doc.name, null); } }"),
					DefaultView.create("by_status",
							"function (doc, meta) { "
									+ "if (doc.type == 'Video' && doc.status) { "
									+ "emit(doc.status, null); "
									+ "} "
									+ "}"),
					DefaultView.create("processed",
							"function (doc, meta) { "
									+ "if (doc.type == 'Video' && doc.status == 'PROCESSED') { "
									+ "emit(doc.status, null); "
									+ "} "
									+ "}"),
					DefaultView.create("no_thumbnails",
							"function (doc, meta) { "
									+ "if (doc.type == 'Video' && typeof doc.thumbNails !== 'undefined') { "
									+ "    if(doc.status == 'NEEDSTHUMBS'){"
									+ "       emit(doc.status, null); "
									+ "     } "
									+ "}"
									+ "}")
					)
			);

	public VideoRepository(CouchbaseConfig videoCouchConfig) {
		config = videoCouchConfig;
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("video_processor_standard");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(vidDoc);
		}


	}

	public JsonDocument save(Video adding) throws Exception {

		if(adding.get_id() == null) throw new Exception("non-null id required");
		
		String json = null;
		try {
			json = mapper.writeValueAsString(adding);
		} catch (JsonProcessingException e) {
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
		JsonDocument inserted = bucket.upsert(doc);
		
		if(inserted.content().getString("_id") == null){
			throw new Exception("ID FIELD REQUIRED");
		}
		return inserted; 
	}

	public Video findOneById(String id) {
		JsonDocument found = bucket.get(id);
		if(found == null){
			return null;
		} else return docToVideo(found);
	}
	public static Video docToVideo(JsonDocument document) {
		JsonObject obj = document.content();
		String objString = obj.toString();

		Video newVid = null;
		try {
			newVid = mapper.readValue( objString , Video.class );
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newVid; 
	}


	public HashSet<Video> getVideosWithStatus(String status){
		ViewQuery query = ViewQuery.from("video_processor_standard", "by_status").inclusiveEnd(true).key(status);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		HashSet<Video> vids = new HashSet<Video>();
		try {
			for (ViewRow row : result) {
				JsonDocument matching = row.document();

				vids.add(docToVideo(matching));
			}

		} catch (Exception e){
			logger.error("error parsing views", e);
		}

		return vids;
	}


	public HashSet<Video> getVideosNeedingThumbs() {
		ViewQuery query = ViewQuery.from("video_processor_standard", "no_thumbnails").limit(20);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		HashSet<Video> vids = new HashSet<Video>();
		try {
			for (ViewRow row : result) {
				JsonDocument matching = row.document();

				vids.add(docToVideo(matching));
			}

		} catch (Exception e){
			logger.error("error parsing views", e);
		}

		return vids;
	}

	public HashSet<Video> findVideosReadyForDeployment() {
		ViewQuery query = ViewQuery.from("video_processor_standard", "processed").limit(20);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		HashSet<Video> vids = new HashSet<Video>();
		try {
			for (ViewRow row : result) {
				JsonDocument matching = row.document();

				vids.add(docToVideo(matching));
			}

		} catch (Exception e){
			logger.error("error parsing views", e);
		}

		return vids;
		
	}


}
