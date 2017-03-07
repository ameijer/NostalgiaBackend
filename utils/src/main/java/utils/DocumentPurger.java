package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.geojson.Feature;
import org.geojson.Point;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nostalgia.persistence.model.KnownLocation;
import com.nostalgia.persistence.model.User;
import com.nostalgia.persistence.model.Video;

import batch.CouchbaseConfig;

public class DocumentPurger {


	public static void main(String[] args) throws Exception{
		Scanner scanner = new Scanner(System.in);
		setupDB();
		System.out.println("Sure you want to purge all docs from bucket?: " + bucket.name() );
		System.out.println("THIS IS IRREVERSIBLE!. Type \"I accept and want to delete\" below" );
		System.out.print("do you accept the irreversibility of this? " );
		String ans = scanner.nextLine();
		if(ans.equals("I accept and want to delete")){
			deleteAllDocs();
		} else {
			System.out.println("nothing deleted");
		}
		
		System.out.println("Goodbye");
	}

	private static void deleteAllDocs() {
		boolean success = bucketManager.flush();
		System.out.println("Succcessful?: " + success);
	}

	// the DB we are using
	private static Cluster cluster; 
	private static  Bucket bucket; 
	private static  CouchbaseConfig config ;
	private static BucketManager bucketManager;

	private static void setupDB() {
		config = new CouchbaseConfig();
		cluster = CouchbaseCluster.create(config.host);
		bucket = cluster.openBucket("REDACTED", "REDACTED");
		bucketManager = bucket.bucketManager();

	}

	
	final static ObjectMapper mapper = new ObjectMapper();
	static{
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}
	


}
