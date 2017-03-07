package batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;

public class BatchProcessRunner {

	final static BatchClass[] batchSources = new BatchClass[] { new NonAwsVideoJsonGetter(), new AwsUrlFixer(),
			new DeletedVideoIdFixer(), new MissingIdFieldFix(), new VideoJsonGetter(), new LocationJsonGetter() };

	// the DB we are using
	private static Cluster cluster;
	private static Bucket bucket;
	private static CouchbaseConfig config;
	private static BucketManager bucketManager;

	private static void setupDB() {
		config = new CouchbaseConfig();
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket(config.bucketName, config.bucketPassword);
		bucketManager = bucket.bucketManager();

	}

	public static void main(String[] args) throws Exception {
		System.out.println("Hello. welcome to the alex batch script runner");

		System.out.println("connecting to db...");
		setupDB();
		System.out.println("The available scripts to execute are: ");

		int index = 0;
		for (BatchClass clazz : batchSources) {
			System.out.println(index + ": " + clazz.getName());
			index++;
		}

		System.out.print("enter number of script to run: ");

		Scanner in = new Scanner(System.in);

		int selection = in.nextInt();

		BatchClass toExecute = batchSources[selection];

		Map<String, JsonDocument> allOfType = new HashMap<String, JsonDocument>();

		int numTypes = 0;
		if (toExecute instanceof VideoBatchClass) {
			numTypes++;
			allOfType.putAll(getAllVideoDocuments());
		}
		if (toExecute instanceof LocationBatchClass) {
			numTypes++;
			allOfType.putAll(getAllLocationDocuments());
		}

		if (toExecute instanceof UserBatchClass) {
			numTypes++;
			allOfType.putAll(getAllUserDocuments());
		}

		if (numTypes < 1) {
			throw new Exception("Batch script: " + toExecute.getName() + " must extend valid superclass");
		}

		Collection<JsonDocument> copied = new ArrayList<JsonDocument>();

		HashMap<String, Integer> originalHashes = new HashMap<String, Integer>();
		for (JsonDocument orig : allOfType.values()) {
			originalHashes.put(orig.id(), orig.hashCode());
			copied.add(JsonDocument.from(orig, orig.id()));
		}

		Set<JsonDocument> modded = batchSources[selection].execute(copied);

		// delete all missing
		for (Iterator<Entry<String, JsonDocument>> iter = allOfType.entrySet().iterator(); iter.hasNext();) {
			JsonDocument orig = iter.next().getValue();
			if (!modded.contains(orig)) {
				// then the document was deleted in the batch script
				deleteDocument(orig);
				iter.remove();
			}
		}

		// save all changed docs

		for (JsonDocument mod : modded) {
			JsonDocument original = allOfType.get(mod.id());

			boolean changed = false;

			int originalHashCode = originalHashes.get(mod.id());
			if (mod.hashCode() != originalHashCode) {
				changed = true;
			}

			if (changed) {
				update(mod);

			}

		}

	}

	private static boolean update(JsonDocument mod) {
		System.out.println("updating document: " + mod.id());
		JsonDocument updated = bucket.upsert(mod);

		return updated.id().equals(mod.id());

	}

	private static boolean deleteDocument(JsonDocument orig) {
		System.out.println("Deleting document: " + orig.id());
		JsonDocument removed = bucket.remove(orig.id());
		return removed.id().equals(orig.id());

	}

	private static Map<String, JsonDocument> getAllUserDocuments() {

		ViewQuery query = ViewQuery.from("user", "all_users");
		ViewResult result = bucket.query(query);
		if (!result.success()) {
			String error = result.error().toString();
			System.err.println("error from view query:" + error);
		}

		if (result == null || result.totalRows() < 1) {
			return null;
		}

		HashMap<String, JsonDocument> users = new HashMap<String, JsonDocument>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();
			if (matching != null)
				users.put(row.id(), JsonDocument.from(matching, row.id()));
		}

		return users;

	}

	private static Map<String, JsonDocument> getAllVideoDocuments() {
		ViewQuery query = ViewQuery.from("video_standard", "by_id");
		ViewResult result = bucket.query(query);
		if (!result.success()) {
			String error = result.error().toString();
			System.err.println("error from view query:" + error);
		}

		if (result == null || result.totalRows() < 1) {
			return null;
		}

		HashMap<String, JsonDocument> videos = new HashMap<String, JsonDocument>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();
			if (matching != null)
				videos.put(row.id(), JsonDocument.from(matching, row.id()));
		}
		return videos;
	}

	private static Map<String, JsonDocument> getAllLocationDocuments() {
		ViewQuery query = ViewQuery.from("location_standard", "by_name");
		ViewResult result = bucket.query(query);
		if (!result.success()) {
			String error = result.error().toString();
			System.err.println("error from view query:" + error);
		}

		if (result == null || result.totalRows() < 1) {
			return null;
		}

		HashMap<String, JsonDocument> locs = new HashMap<String, JsonDocument>();
		for (ViewRow row : result) {
			JsonDocument matching = row.document();
			if (matching != null)
				locs.put(row.id(), JsonDocument.from(matching, row.id()));
		}
		return locs;
	}
}
