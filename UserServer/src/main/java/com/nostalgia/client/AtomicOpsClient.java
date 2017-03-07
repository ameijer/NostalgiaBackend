package com.nostalgia.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.StringDocument;
import com.nostalgia.CouchbaseConfig;

public class AtomicOpsClient {

	//todo make atomics bucket
	private final Bucket bucket;
	private CouchbaseConfig config;
	private CouchbaseCluster cluster; 
	
	public AtomicOpsClient(CouchbaseConfig conf){
		config = conf;
		cluster = CouchbaseCluster.create(conf.host);
		bucket = cluster.openBucket(conf.bucketName, conf.bucketPassword);
	
	}
	
	public long incrementCounter(String counterId){
		 JsonLongDocument rv = bucket.counter(counterId, 1, 0);
		 return rv.content();
	}
	
	public long decrementCounter(String counterId){
	
		 JsonLongDocument rv = bucket.counter(counterId, -1, 0);
		 return rv.content();
	}
	private static final Logger logger = LoggerFactory.getLogger(AtomicOpsClient.class.getName()); 
	public boolean addPrependedItem(String trackerId, String voterId, long voteTime){
		StringDocument existing = bucket.get(trackerId, StringDocument.class); 
		if(existing == null){
		StringDocument initial = StringDocument.create(trackerId, "<created " + System.currentTimeMillis() + ">");
		try {
		bucket.insert(initial);
		} catch (Exception e){
			String msg = e.getMessage(); 
			logger.error("error inserting new doc: ", e);
			return false;
		}
		}
		
		StringDocument toPrepend = StringDocument.create(trackerId, "{" + voterId + ", " + voteTime +"}");
	
		StringDocument prepended = bucket.prepend(toPrepend);

		String contents = bucket.get(prepended).content(); 
		logger.info("document contents: " + contents);
		return prepended.id().equals(toPrepend.id());
	}

	public Object getContents(String idOfTracker) {
		StringDocument doc = bucket.get(idOfTracker, StringDocument.class);
		if(doc == null){
			return null; 
		}
		return doc.content();
	}
	
	
	
	
}
