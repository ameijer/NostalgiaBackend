package com.nostalgia.client;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nostalgia.SyncConfig;
import com.nostalgia.persistence.model.SyncSessionCreateRequest;
import com.nostalgia.persistence.model.SyncSessionCreateResponse;
import com.nostalgia.persistence.model.SyncUser;
import com.nostalgia.persistence.model.User;

public class SynchClient {
	private final SyncConfig conf;
	private final Client sComm; 
	private static final Logger logger = LoggerFactory.getLogger(SynchClient.class.getName().toLowerCase());

	private static final ObjectMapper mapper = new ObjectMapper();
	public SynchClient(SyncConfig syncConfig, Client jClient) {
		conf = syncConfig;
		sComm = jClient;

	}

	public boolean registerUser(User loggedIn) {
		UriBuilder uribuild = UriBuilder.fromUri("http://" + conf.host + ":" + conf.port + conf.addUserPath);



		//name to be last part of id
		String name = loggedIn.get_id().substring(loggedIn.get_id().lastIndexOf("-"));

		SyncUser copy = new SyncUser(); 

	
		copy.setName(name);
		copy.setTtl(30 * 24 * 3600);
		
		Response resp = null; 
		try {
			resp = sComm.target(uribuild).request().post(Entity.json(copy));

			logger.info("response: " + resp.getStatus());
		} catch (Exception e){
			logger.error("error registering new user", e);
			return false;
		} finally {
			if(resp != null){
				resp.close();
			}
		}



		return true;
	}

	public SyncSessionCreateResponse createSyncSessionFor(User loggedIn) {
		UriBuilder uribuild = UriBuilder.fromUri("http://" + conf.host + ":" + conf.port + conf.newSessionPath);
		String name = loggedIn.get_id().substring(loggedIn.get_id().lastIndexOf("-"));
		SyncSessionCreateRequest req = new SyncSessionCreateRequest();
		req.setName(name);
		SyncSessionCreateResponse syncResp = null;
		Response resp = null;
		try {
			Builder build = sComm.target(uribuild).request();
			resp = build.post(Entity.json(req));

			syncResp = resp.readEntity(SyncSessionCreateResponse.class);
			resp.close();
		
			if(syncResp.getSession_id() == null) return null; 

		} catch (Exception e){
			logger.info("error creating sync session for user");
			return null;
		} finally {
			if(resp != null){
				resp.close();
			}
		}
		return syncResp;
	}

	public boolean setSyncChannels(User hasNewLoc) {

		String name = hasNewLoc.get_id().substring(hasNewLoc.get_id().lastIndexOf("-"));
		UriBuilder uribuild = UriBuilder.fromUri("http://" + conf.host + ":" + conf.port + conf.getUserPath + name);

		HttpGet httpGet = new HttpGet("http://" + conf.host + ":" + conf.port + conf.getUserPath + name);


		httpGet.setHeader("Content-type", "application/json");


		HttpResponse resp = null;
		try {
			resp = new DefaultHttpClient().execute(httpGet);
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		String contents = null;
		User existing = null;
		try {
			contents = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");

			existing = mapper.readValue(contents, User.class);
		} catch (Exception e) {

			e.printStackTrace();
		}


		boolean changed = false;

		if(existing.getAdmin_channels().size() == hasNewLoc.getAdmin_channels().size()){
			for(String channel : hasNewLoc.getAdmin_channels()){
				if(!existing.getAdmin_channels().contains(channel)){
					changed = true;
					break; 
				}
			}
		} else {
			changed = true;
		}

		if(changed){
			existing.setAdmin_channels(hasNewLoc.getAdmin_channels());

			Response ack = sComm.target(uribuild).request().put(Entity.json(existing));
			ack.close();
			return true;
		} else return false;

	}

}
