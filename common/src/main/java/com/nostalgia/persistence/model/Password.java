package com.nostalgia.persistence.model;

import java.io.Serializable;
import java.util.UUID;

public class Password implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 90833290106063542L;

	private String _id = UUID.randomUUID().toString();
	private String ownerId; 
	private String dateChanged;
	private String password; 

	private int version; 
	
	public Password() {
		super();

	}

	public Password(String password, String _id, String ownerId, String dateChanged) {
		super();
		if(_id != null){
			this._id = _id;
		}
		this.ownerId = ownerId;
		this.dateChanged = dateChanged;
		this.password = password; 
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getDateChanged() {
		return dateChanged;
	}

	public void setDateChanged(String dateChanged) {
		this.dateChanged = dateChanged;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	} 



}
