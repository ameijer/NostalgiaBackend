package com.nostalgia.contentserver.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nostalgia.contentserver.CouchbaseConfig;

import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;


public class ContentServConfig extends Configuration{

	@Valid
	@NotNull
	@JsonProperty("VideoCouch")
	private CouchbaseConfig videoCouch = new CouchbaseConfig();
	

	@Valid
	@NotNull
	@JsonProperty("jerseyClient")
	private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();
	
	@Valid
	@NotNull
	@JsonProperty("DataConfig")
	private DataConfig dataConfig = new DataConfig();

	
	public DataConfig getDataConfig() {
		return dataConfig;
	}

	public JerseyClientConfiguration getJerseyClientConfiguration() {
		return jerseyClient;
	}

	public CouchbaseConfig getVideoCouch(){
		return videoCouch; 
	}
}
