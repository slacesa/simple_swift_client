package com.slacesa.simpleSwiftClient.resources;

/**
 * POJO for Swift Config
 * @author SLC
 *
 */
public class SwiftConfig {

	private String
	username,
	password,
	auth_host,
	auth_endpoint,
	storage_host,
	storage_endpoint;
	
	private int port;

	public SwiftConfig() {}

	/**
	 * Username for Keystone v3 authentication
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Password for Keystone v3 authentication
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Host URL for Keystone v3 authentication
	 * @return hostname
	 */
	public String getAuth_host() {
		return auth_host;
	}

	public void setAuth_host(String auth_host) {
		this.auth_host = auth_host;
	}

	/**
	 * Endpoint for Keystone v3 authentication
	 * @return endpoint
	 */
	public String getAuth_endpoint() {
		return auth_endpoint;
	}

	public void setAuth_endpoint(String auth_endpoint) {
		this.auth_endpoint = auth_endpoint;
	}

	/**
	 * Storage URL for Keystone v3 authentication
	 * @return hostname
	 */
	public String getStorage_host() {
		return storage_host;
	}

	public void setStorage_host(String storage_host) {
		this.storage_host = storage_host;
	}

	/**
	 * Storage endpointfor Keystone v3 authentication
	 * @return endpoint
	 */
	public String getStorage_endpoint() {
		return storage_endpoint;
	}

	public void setStorage_endpoint(String storage_endpoint) {
		this.storage_endpoint = storage_endpoint;
	}

	/**
	 * Port for both hosts, if 443 it will assume SSL is ON
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}