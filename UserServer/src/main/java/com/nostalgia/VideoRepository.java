package com.nostalgia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import flexjson.JSONDeserializer;

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
			"video_standard",
			Arrays.asList(
					DefaultView.create("by_id",
							"function (doc, meta) { if (doc.type == 'Video') { emit(doc._id, null); } }"),
	


					DefaultView.create("by_status",
							"function (doc, meta) { "
									+ "if (doc.type == 'Video' && doc.status) { "
									+ "emit(doc.status, null); "
									+ "} "
									+ "}")
					)
			);

	// Initialize design document
	DesignDocument spatialDoc = DesignDocument.create(
			"video_spatial",
			Arrays.asList(
					SpatialView.create("video_points",
							"function (doc, meta) { "
									+ "if (doc.type == 'Video' && doc.location.coordinates) { "
									+ " emit(doc.location, null);"
									+ "}"
									+ "}")
					)
			);

	public VideoRepository(CouchbaseConfig videoCouchConfig) {
		config = videoCouchConfig;
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("video_standard");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(vidDoc);
		}

		existing = bucketManager.getDesignDocument("video_spatial");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(spatialDoc);
		}

	}

	public JsonDocument save(Video adding) throws Exception {

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

	public Video findOneById(String id) {
		ViewQuery query = ViewQuery.from("video_standard", "by_id").inclusiveEnd(true).key(id);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}
	

		if (result == null || result.totalRows() < 1){
			return null;
		}
		
		ArrayList<Video> vids = new ArrayList<Video>();
		for (ViewRow row : result) {
		    JsonDocument matching = row.document();
		    
		    vids.add(docToVideo(matching));
		}

		if(vids.size() > 1){
			logger.error("TOO MANY vids MATCHING id");
		}
		if(vids.size() < 1) return null; 
		return vids.get(0);
	}
	
	public static Video docToVideo(JsonDocument document) {
		JsonObject obj = document.content();
		String objString = obj.toString();

		Video newVid = null;
		try {
			newVid = mapper.readValue( objString , Video.class );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newVid; 
	}

	public HashMap<String, Video> findVideosWithin(Polygon hasbbox) {
		double[] bbox = hasbbox.getBbox(); 

		if(bbox == null){
			bbox = LocationRepository.buildbbox(hasbbox);
		}
		if(bbox == null || bbox.length < 4){
			logger.error("only bounding box based queries supported at this time");
			return null;
		}
		JsonArray START = JsonArray.from(bbox[1], bbox[0]);
		JsonArray END = JsonArray.from(bbox[3], bbox[2]);
		SpatialViewQuery query = SpatialViewQuery.from("video_spatial", "video_points").range(START, END);
		SpatialViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
			return null;
		}

		List<SpatialViewRow> rows = result.allRows();

		HashMap<String, Video> s = new HashMap<String, Video>();
		for (SpatialViewRow row : rows) {
			JsonDocument matching = row.document();
			s.put(matching.id().substring(0, 8), docToVideo(matching));
		}
		return s;
	}
}
