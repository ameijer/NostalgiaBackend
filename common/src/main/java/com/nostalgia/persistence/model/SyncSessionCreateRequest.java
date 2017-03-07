package com.nostalgia.persistence.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncSessionCreateRequest implements Serializable{

	/**
	 * 
	 */
	@JsonIgnore
	private static final long serialVersionUID = 5641094760223844297L;
	
	
	private String name;
	private int ttl = 160000;
	
	
	public SyncSessionCreateRequest() {
		super();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getTtl() {
		return ttl;
	}
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}
	
	
	

}
