package io.github.slacesa.simpleSwiftClient.resources;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Very simple and basic Authentication Message for Keystone v3
 * @author SLC
 *
 */
public class AuthMessage {

	private JsonObject auth;

	public AuthMessage(String name, String password) {
		this.auth = new JsonObject()
				.put("identity", JsonObject.mapFrom(new Identity(name, password)));
	}

	public JsonObject parse() {
		return new JsonObject().put("auth", auth);
	}
}

class Identity {
	public JsonArray methods;
	public JsonObject password;
	
	public Identity() {
		
	}

	public Identity(String name, String password) {
		this.methods = new JsonArray()
				.add("password");
		this.password = new JsonObject()
				.put("user",JsonObject.mapFrom(new User(name, password)));
	}
}

class User {
	public String name;
	public JsonObject domain;
	public String password;

	public User() {
		
	}

	public User(String name, String password) {
		this.name = name;
		this.domain = new JsonObject().put("name", "Default");
		this.password = password;
	}
}