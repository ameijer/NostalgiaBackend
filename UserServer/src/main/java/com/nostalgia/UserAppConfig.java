package com.nostalgia;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.client.JerseyClientConfiguration;

public class UserAppConfig extends Configuration{
	
	@Valid
	@NotNull
	@JsonProperty("UserCouch")
	private CouchbaseConfig userServConfig = new  CouchbaseConfig();

	public CouchbaseConfig getUserServerConfig() {
		return userServConfig;
	}
	
	@Valid
	@NotNull
	@JsonProperty("SyncServer")
	private SyncConfig syncConfig = new SyncConfig();

	public SyncConfig getSyncConfig() {
		return syncConfig;
	}

	@Valid
	@NotNull
	@JsonProperty("jerseyClient")
	private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

	
	public JerseyClientConfiguration getJerseyClientConfiguration() {
		return jerseyClient;
	}

	
	@Valid
	@NotNull
	@JsonProperty("LocationCouch")
	private CouchbaseConfig locationServConfig = new  CouchbaseConfig();

	public CouchbaseConfig getLocationServerConfig() {
		return locationServConfig;
	}
	
	@Valid
	@NotNull
	@JsonProperty("VideoCouch")
	private CouchbaseConfig videoServConfig = new  CouchbaseConfig();

	public CouchbaseConfig getVideoCouchConfig() {
		return videoServConfig;
	}
	
	@Valid
	@NotNull
	@JsonProperty("MediaCollectionCouch")
	private CouchbaseConfig collectionServConfig = new  CouchbaseConfig();

	public CouchbaseConfig getCollectionServerConfig() {
		return collectionServConfig;
	}
	
	
	@Valid
	@NotNull
	@JsonProperty("AtomicsCouch")
	private CouchbaseConfig atomicsServConfig = new  CouchbaseConfig();

	@Valid
	@NotNull
	@JsonProperty("PassCouch")
	private CouchbaseConfig passServConfig = new  CouchbaseConfig();
	
	public CouchbaseConfig getAtomicsServerConfig() {
		return atomicsServConfig;
	}

	@Valid
    @NotNull
    @JsonProperty("httpClient")
    private HttpClientConfiguration httpClient = new HttpClientConfiguration();

   
    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }


	public CouchbaseConfig getPasswordServerConfig() {
		return passServConfig; 
	}

	@Valid
	@NotNull
	@JsonProperty("TokenRepo")
	private CouchbaseConfig tokenServConfig = new  CouchbaseConfig();

	public CouchbaseConfig getTokenRepoConfig() {
		
		return tokenServConfig;
	}

	
}