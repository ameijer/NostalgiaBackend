package com.nostalgia.persistence.model;

import java.io.Serializable;

public class SyncUser implements Serializable{

	public String name;
	
	public long ttl;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getTtl() {
		return ttl;
	}

	public void setTtl(long ttl) {
		this.ttl = ttl;
	}

	public SyncUser() {
		super();
	}
	
}
