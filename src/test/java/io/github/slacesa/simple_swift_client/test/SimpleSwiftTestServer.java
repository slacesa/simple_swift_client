package io.github.slacesa.simple_swift_client.test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import io.github.slacesa.simpleSwiftClient.resources.AuthMessage;
import io.github.slacesa.simpleSwiftClient.resources.PolicyRetrievalStates;
import io.github.slacesa.simpleSwiftClient.resources.SwiftConfig;
import io.github.slacesa.simpleSwiftClient.resources.SwiftFile;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * A Simple Swift Test Server that simulates basic Keystone v3 authentication and actions with storage
 * @author SLC
 *
 */
public class SimpleSwiftTestServer {

	private static final Logger log = Logger.getLogger(SimpleSwiftTestServer.class.getName());
	private static HttpServer server;
	private static SimpleSwiftTestServer thisServer;

	private Vertx vertx;
	private SwiftConfig config;
	private String testToken;
	private List<SwiftFile> fileList;

	private SimpleSwiftTestServer(Vertx vertx, SwiftConfig config) {
		this.vertx = vertx;
		this.config = config;
		this.testToken = "49nif938f3j9ij94ihif09u3fu4ih34fh99h43hf9h";
	}

	public static Future<HttpServer> getTestServer(Vertx vertx, SwiftConfig config) {
		if(thisServer == null) {
			log.fine("Launching Web Server");
			thisServer = new SimpleSwiftTestServer(vertx, config);
			return thisServer.initTestServer();
		}
		else return Future.succeededFuture(server); 
	}

	private Future<HttpServer> initTestServer() {
		Promise<HttpServer> result = Promise.promise();

		vertx.fileSystem().readFile("src/test/data/testsample.json", ar -> {
			this.fileList = null;
			
			try {
				if(ar.succeeded())
					this.fileList = Arrays.asList(Json.decodeValue(ar.result(), SwiftFile[].class));
			}
			catch (Exception e) {}
		});

		Router router = Router.router(vertx);
		router.get("/").handler(context -> {
			context.response().end("Hello Test");
		});
		router.route(config.getAuth_endpoint()+"*").handler(BodyHandler.create());
		router.post(config.getAuth_endpoint()).handler(this::auth);
		
		router.route(config.getStorage_endpoint()+"*").handler(BodyHandler.create());
		router.get(config.getStorage_endpoint()).handler(this::getList);
		
		router.route(config.getStorage_endpoint()+"/*").handler(BodyHandler.create());
		router.get(config.getStorage_endpoint()+"/*").handler(this::getFile);
		router.put(config.getStorage_endpoint()+"/*").handler(this::putFile);
		router.delete(config.getStorage_endpoint()+"/*").handler(this::deleteFile);

		vertx.createHttpServer().requestHandler(router)
		.listen(config.getPort(), ar -> {
			if(ar.succeeded()) {
				server = ar.result();
				result.complete(server);
				log.fine("Web server launched");
			}
			else result.fail(ar.cause());
		});
		return result.future();
	}

	private void auth(RoutingContext routingContext) {
		JsonObject request = routingContext.getBodyAsJson();
		if(new AuthMessage(config.getUsername(), config.getPassword()).parse().equals(request)) {
			routingContext.response()
			.putHeader("Content-Type", "application/json")
			.putHeader("X-Subject-Token", testToken)
			.end(new JsonObject()
					.put("token", new JsonObject().
							put("expires_at", DateTime.now().plusDays(1).toString())).encode());
		}
		else {
			routingContext.response()
			.putHeader("Content-Type", "application/json")
			.setStatusCode(400)
			.end(new JsonObject()
					.put("error", new JsonObject())
					.put("message", "Not authorized").encode());
		}
		
	}

	private void getList(RoutingContext routingContext) {
		if(!testToken.equals(routingContext.request().getHeader("X-Auth-Token"))) routingContext.fail(400); 
		else if(fileList == null) routingContext.fail(404);
		else routingContext.response().setStatusCode(200).end(Json.encodeToBuffer(fileList));
	}

	private void getFile(RoutingContext routingContext) {
		if(!testToken.equals(routingContext.request().getHeader("X-Auth-Token"))) {
			routingContext.fail(400);
			return;
		}
		// TODO Could also check the validity of headers, as checksum,  
		String filename = null;
		for(String p : routingContext.normalizedPath().split("/")) {
			filename = p;
		}
		boolean found = false;
		for(SwiftFile file : fileList) {
			if(filename!=null && filename.equals(file.getName())) {
				found = true;
				if(file.getPolicy_retrieval_state().equals(PolicyRetrievalStates.UNSEALED.getValue()))
						routingContext.response().setStatusCode(200).end(Buffer.buffer(new byte[100]));
				else routingContext.response().setStatusCode(429).putHeader("Retry-After", "1000").end();
			}
		}
		if(!found) routingContext.response().setStatusCode(404).end();
	}

	private void putFile(RoutingContext routingContext) {
		if(!testToken.equals(routingContext.request().getHeader("X-Auth-Token"))) {
			routingContext.fail(400);
			return;
		}
		routingContext.response().setStatusCode(201).end();
	}

	private void deleteFile(RoutingContext routingContext) {
		if(!testToken.equals(routingContext.request().getHeader("X-Auth-Token"))) {
			routingContext.fail(400);
			return;
		}
		String filename = null;
		for(String p : routingContext.normalizedPath().split("/")) {
			filename = p;
		}
		boolean found = false;
		for(SwiftFile file : fileList) {
			if(filename!=null && filename.equals(file.getName())) {
				found = true;
				routingContext.response().setStatusCode(204)
				.putHeader("Content-Length", "0")
				.putHeader("X-Trans-Id", "txcf8e98d30f714c85a323d-0058c16813")
				.putHeader("X-Openstack-Request-Id", "txcf8e98d30f714c85a323d-0058c16813")
				.putHeader("Date",new DateTime().toString())
				.end();
			}
		}
		if(!found) routingContext.response().setStatusCode(404).end();
	}
}