package batch;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.persistence.model.Video;

public class DeletedVideoIdFixer extends BatchClass implements UserBatchClass, LocationBatchClass {

	@Override
	public String getName() {
		return "missing video id fixer";
	}

	// the DB we are using
	private static Cluster cluster;
	private static Bucket bucket;
	private static CouchbaseConfig config;
	private static BucketManager bucketManager;

	private static ObjectMapper mapper = new ObjectMapper();

	private static void setupDB() {
		config = new CouchbaseConfig();
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();

	}

	@Override
	public Set<JsonDocument> execute(Collection<JsonDocument> input) {
		setupDB();
		HashSet<JsonDocument> toSave = new HashSet<JsonDocument>();

		for (JsonDocument original : input) {

			String type = original.content().getString("type");

			switch (type) {
			case "User":
				// scrubExpiredVidsFromUser(original.content());
				break;
			case "KnownLocation":
				try {
					scrubExpiredVidsFromLocation(original.content());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			default:
				System.err.println("error inferring type of document: " + original.id());
				System.exit(1);
			}

			// add to set so that updates get saved
			toSave.add(original);
		}
		return toSave;
	}

	private void scrubExpiredVidsFromLocation(JsonObject content) throws Exception {
		// for all fields with video ids, check that video actually exists
		// if not, remove

		Object sponsored = content.get("sponsoredVideos");
		Object matchingVids = content.get("matchingVideos");

		if (sponsored != null) {
			System.err.println("support for sponsored videos not yet implemented");
			System.exit(1);
		}

		if (matchingVids != null) {
			Map<String, String> matching = null;
			try {
				matching = mapper.readValue(matchingVids.toString(), new TypeReference<Map<String, String>>() {
				});
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Iterator<Entry<String, String>> iter = matching.entrySet().iterator();

			while (iter.hasNext()) {
				Entry<String, String> ofInterest = iter.next();

				String idToCheck = ofInterest.getValue();

				JsonDocument existing = bucket.get(idToCheck);

				if (existing == null) {
					iter.remove();
				}

			}

			content.put("matchingVideos", mapper.writeValueAsString(matching));

		}
	}

}
