package com.nostalgia;

import javax.validation.constraints.NotNull;

public class SyncConfig {

	public String newSessionPath = "/sync_gateway/_session";

	@NotNull
	public String host;
	
	@NotNull
	public int port;

	public String addUserPath = "/sync_gateway/_user/";

	public String getUserPath = "/sync_gateway/_user/";




}
