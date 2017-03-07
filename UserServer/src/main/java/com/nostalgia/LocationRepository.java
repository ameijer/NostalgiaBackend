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
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;
import com.couchbase.client.java.view.*;
import flexjson.JSONDeserializer;

public class LocationRepository {

	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();


	private static final Logger logger = LoggerFactory.getLogger(LocationRepository.class);

	public KnownLocation findOneById(String id) throws Exception {
		ViewQuery query = ViewQuery.from("location_standard", "by_id").inclusiveEnd(true).key(id);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		ArrayList<KnownLocation> locs = new ArrayList<KnownLocation>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			locs.add(docToLocation(matching));
		}

		if(locs.size() > 1){
			logger.error("TOO MANY locations MATCHING ID");
		}
		if(locs.size() < 1) return null; 
		return locs.get(0);


	}

	// Initialize design document
	DesignDocument locDoc = DesignDocument.create(
			"location_standard",
			Arrays.asList(
					DefaultView.create("by_name",
							"function (doc, meta) { if (doc.type == 'KnownLocation') { emit(doc.name, null); } }"),
					DefaultView.create("by_id",
							"function (doc, meta) { if (doc.type == 'KnownLocation') { emit(doc._id, null); } }"),
					DefaultView.create("by_channel",
							"function (doc, meta) { "
									+ "if (doc.type == 'KnownLocation') { "
									+ "for (i=0; i < doc.channels.length; i++) {"
									+ "emit(doc.channels[i], null); "
									+ "} "
									+ "} "
									+ "}")
					)
			);

	// Initialize design document
	DesignDocument spatialDoc = DesignDocument.create(
			"location_spatial",
			Arrays.asList(
					SpatialView.create("known_points",
							"function (doc, meta) { "
									+ "if (doc.type == 'KnownLocation' && doc.location) { "
									+ " emit(doc.location.geometry, null);"
									+ "}"
									+ "}"),
					SpatialView.create("discrete_location_points",
							"function (doc, meta) { "
									+ "if (doc.type == 'KnownLocation' && doc.location && doc.location.geometry && doc.location.geometry.type == 'GeometryCollection') { "
									+ "     var arrayLength = doc.location.geometry.geometries.length;" 
									+ "     for(var i = 0; i < arrayLength; i++) {"
									+ "         if(doc.location.geometry.geometries[i].type == 'Point') {"
									+ "             emit(doc.location.geometry.geometries[i], i);"
									+ "         }"
									+ "     }"
									+ "}"
									+ "}")
					)
			);


	public LocationRepository(CouchbaseConfig userCouchConfig) {
		config = userCouchConfig;
		cluster = CouchbaseCluster.create(userCouchConfig.host);
		bucket = cluster.openBucket(userCouchConfig.bucketName, userCouchConfig.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("location_standard");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(locDoc);
		}

		existing = bucketManager.getDesignDocument("location_spatial");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(spatialDoc);
		}


	}

	public static double[] buildbbox(Polygon poly){
		double minLong = 180;
		double maxLong = -180;
		double minLat = 180;
		double maxLat = -180;

		if(poly instanceof Polygon){
			Polygon focused = (Polygon) poly;
			focused.getCoordinates();

			for(int i = 0; i < focused.getCoordinates().get(0).size(); i ++){
				LngLatAlt cur = focused.getCoordinates().get(0).get(i);
				double curLat = cur.getLatitude();
				double curLong = cur.getLongitude();

				if(curLong < minLong){
					minLong = curLong;
				}

				if(curLong > maxLong){
					maxLong = curLong; 
				}

				if(curLat < minLat){
					minLat = curLat;
				}

				if(curLat > maxLat){
					maxLat = curLat; 
				}

			}


		} else {
			logger.error("non-polygon supplied in geo object, unable to create query");
			return null;
		}

		return new double[]{minLong, minLat, maxLong, maxLat};
	}
	public HashMap<String, KnownLocation> findKnownLocationsCoveringArea(Feature newLoc) {
		GeoJsonObject rawGeo = newLoc.getGeometry();
		Polygon toBuildbbox = null;

		if(rawGeo instanceof GeometryCollection){
			GeometryCollection geoColl = (GeometryCollection) rawGeo;
			for(GeoJsonObject member : geoColl.getGeometries()){
				if(member instanceof Polygon){
					toBuildbbox = (Polygon) member;
					break;
				}
			}

		} else {
			toBuildbbox = (Polygon) rawGeo;
		}


		if(toBuildbbox == null){
			logger.error("null geometry object supplied, unable to perform query");
			return null;
		}

		double[] bbox = buildbbox(toBuildbbox);

		JsonArray START = JsonArray.from(bbox[0], bbox[2]);
		JsonArray END = JsonArray.from(bbox[1], bbox[3]);
		SpatialViewQuery query = SpatialViewQuery.from("location_spatial", "known_points").range(START, END);
		SpatialViewResult result = bucket.query(query/*.key(name).limit(10)*/);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		List<SpatialViewRow> rows = result.allRows();


		HashMap<String, KnownLocation> s = new HashMap<String, KnownLocation>();
		for (SpatialViewRow row : rows) {
			JsonDocument matching = row.document();
			if(matching == null) continue;
			s.put(matching.id().substring(0, 8), docToLocation(matching));
		}
		return s;
	}

	public List<KnownLocation> findByName(String name) throws Exception {
		ViewQuery query = ViewQuery.from("location_standard", "by_name").inclusiveEnd(true).key(name);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		if (result == null || result.totalRows() < 1){
			return null;
		}

		ArrayList<KnownLocation> locs = new ArrayList<KnownLocation>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();

			locs.add(docToLocation(matching));
		}

		if(locs.size() > 1){
			logger.error("TOO MANY locations MATCHING NAME");
		}
		return locs;

	}

	public static KnownLocation docToLocation(JsonDocument document) {
		JsonObject obj = document.content();
		String objString = obj.toString();
		
		KnownLocation knownLoc = null;
		try {
			knownLoc = mapper.readValue( objString , KnownLocation.class );
		} catch (Exception e) {
			logger.error("error converting location to doc", e);
		}

		return knownLoc; 
	}

	public HashSet<KnownLocation> findByChannel(String channel) {
		HashSet<KnownLocation> s = (HashSet<KnownLocation>) Collections.synchronizedSet(new HashSet<KnownLocation>());

		ViewQuery query = ViewQuery.from("location_standard", "by_channel").inclusiveEnd(true).key(channel);
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

			s.add(docToLocation(matching));
		}

		return s;
	}

	public synchronized JsonDocument save(KnownLocation loc) {
		//fix channel name
		loc.getChannels().clear();
		loc.getChannels().add(loc.getChannelName());


		String json = null;

		try {
			json = mapper.writeValueAsString(loc);
		} catch (JsonProcessingException e) {

			e.printStackTrace();
		}

		JsonObject jsonObj = JsonObject.fromJson(json);

		JsonDocument  doc = JsonDocument.create(loc.get_id(), jsonObj);



		JsonDocument inserted = bucket.upsert(doc);
		return inserted;
	}
	
	public JsonDocument remove(KnownLocation toDelete) {
		// Remove the document and make sure the delete is persisted.
		JsonDocument doc = bucket.remove(toDelete.get_id());
		return doc;
	}
	
	public HashMap<String, KnownLocation> findKnownLocationsCoveringPoint(Point newLoc) {
		JsonArray START = JsonArray.from(newLoc.getCoordinates().getLongitude(), newLoc.getCoordinates().getLatitude());
		JsonArray END = JsonArray.from(newLoc.getCoordinates().getLongitude(), newLoc.getCoordinates().getLatitude());
		SpatialViewQuery query = SpatialViewQuery.from("location_spatial", "known_points").range(START, END);
		SpatialViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}


		List<SpatialViewRow> rows = result.allRows();


		HashMap<String, KnownLocation> s = new HashMap<String, KnownLocation>();
		for (SpatialViewRow row : rows) {
			JsonDocument matching = row.document();
			s.put(matching.id().substring(0, 8), docToLocation(matching));
		}
		return s;
	}

	public List<KnownLocation> findDiscreteLocationsInBbox(JsonArray bboxArray) {

		if(bboxArray == null || bboxArray.size() < 4){
			logger.error("only bounding box based queries supported at this time");
			return null;
		}

		JsonArray START = JsonArray.from(bboxArray.get(0), bboxArray.get(1));
		JsonArray END = JsonArray.from(bboxArray.get(2), bboxArray.get(3));
		SpatialViewQuery query = SpatialViewQuery.from("location_spatial", "discrete_location_points").range(START, END);
		SpatialViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
			return null;
		}

		List<SpatialViewRow> rows = result.allRows();

		List<KnownLocation> s = new ArrayList<KnownLocation>();
		for (SpatialViewRow row : rows) {
			JsonDocument matching = row.document();
			s.add(docToLocation(matching));
		}
		return s;
	}


}
