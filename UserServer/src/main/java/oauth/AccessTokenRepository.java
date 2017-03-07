package oauth;
import flexjson.JSONDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;

import org.joda.time.DateTime;
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
import com.google.common.base.Optional;
import com.nostalgia.CouchbaseConfig;
import com.nostalgia.persistence.model.Password;
import com.nostalgia.persistence.model.User;

public class AccessTokenRepository {

	// the DB we are using
	private final Cluster cluster; 
	private final Bucket bucket; 
	private final CouchbaseConfig config;
	private final BucketManager bucketManager;
	private static final ObjectMapper mapper = new ObjectMapper();
	

	private static final Logger logger = LoggerFactory.getLogger(AccessTokenRepository.class);
	

	
	// Initialize design document
	DesignDocument tokenDoc = DesignDocument.create(
		"token",
		Arrays.asList(
			DefaultView.create("by_owner_id",
				"function (doc, meta) { emit(doc.user_id, null); }"),
			DefaultView.create("by_expiration_date",
					"function (doc, meta) { emit(doc.last_access_utc, null); }"),
			DefaultView.create("by_token_id",
				"function (doc, meta) { emit(doc.access_token_id, null); }")
		)
	);
	
	public AccessTokenRepository(CouchbaseConfig tokenCouchConfig) {
		config = tokenCouchConfig;
		cluster = CouchbaseCluster.create(tokenCouchConfig.host);
		bucket = cluster.openBucket(tokenCouchConfig.bucketName, tokenCouchConfig.bucketPassword);
		bucketManager = bucket.bucketManager();
		DesignDocument existing = bucketManager.getDesignDocument("token");
		if(existing == null){
			// Insert design document into the bucket
			bucketManager.insertDesignDocument(tokenDoc);
		}
	}
	
	public Optional<AccessToken> findAccessTokenById(final UUID accessTokenId) {
		AccessToken accessToken = getTokenById(accessTokenId.toString());
		if (accessToken == null) {
			return Optional.absent();
		}
		return Optional.of(accessToken);
	}

	private AccessToken getTokenById(String string) {
		ViewQuery query = ViewQuery.from("token", "by_token_id").inclusiveEnd(true).key(string);
		ViewResult result = bucket.query(query);
		if(!result.success()){
			String error = result.error().toString();
			logger.error("error from view query:" + error);
		}
	

		if (result == null || result.totalRows() < 1){
			return null;
		}
		
		ArrayList<AccessToken> tokens = new ArrayList<AccessToken>();
		for (ViewRow row : result) {
		    JsonDocument matching = row.document();
		    
		    tokens.add(docToToken(matching));
		}

		if(tokens.size() > 1){
			logger.error("TOO MANY tokens MATCHING id");
		}
		if(tokens.size() < 1) return null; 
		return tokens.get(0);
	}

	public AccessToken generateNewAccessToken(final User user, final long dateTime) {
		AccessToken accessToken = new AccessToken(UUID.randomUUID(), user.get_id(), dateTime);
		try {
		save(accessToken);
	} catch (Exception e) {
		logger.error("Error generating new access token: " + accessToken, e);
		return null;
	} 
		return accessToken;
	}
	
	public synchronized JsonDocument save(AccessToken token) throws Exception {


		String json = null;
		try {
			json = mapper.writeValueAsString(token);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JsonObject jsonObj = JsonObject.fromJson(json);

		JsonDocument  doc = JsonDocument.create(token.getAccess_token_id().toString(), jsonObj);
		

		JsonDocument inserted = bucket.upsert(doc);
		
		return inserted;
	}

	public void setLastAccessTime(final UUID accessTokenUUID, final long dateTime) {
		AccessToken accessToken = findAccessTokenById(accessTokenUUID).orNull(); 
		AccessToken updatedAccessToken = accessToken.setLast_access_utc(dateTime);
		try {
			save(updatedAccessToken);
		} catch (Exception e) {
			logger.error("Error setting last access time for token: " + accessTokenUUID, e); 
		} 
	}
	
	

	public AccessToken docToToken(JsonDocument document) {
		if(document == null) return null;
		
		JsonObject obj = document.content();
		String objString = obj.toString();
		
		AccessToken newToken = null;
		try {
			newToken = mapper.readValue(objString, AccessToken.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return newToken; 
	}
}
