package com.nostalgia.persistence.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncSessionCreateResponse implements Serializable{

	/**
	 * 
	 */
	@JsonIgnore
	private static final long serialVersionUID = 5641094760223843297L;
	
	
	private String session_id;
	private String cookie_name;
	private String expires;
	
	
	public SyncSessionCreateResponse() {
		super();
	}


	public String getSession_id() {
		return session_id;
	}


	public void setSession_id(String session_id) {
		this.session_id = session_id;
	}


	public String getCookie_name() {
		return cookie_name;
	}


	public void setCookie_name(String cookie_name) {
		this.cookie_name = cookie_name;
	}


	public String getExpires() {
		return expires;
	}


	public void setExpires(String expires) {
		this.expires = expires;
	}
	

	
	

}
