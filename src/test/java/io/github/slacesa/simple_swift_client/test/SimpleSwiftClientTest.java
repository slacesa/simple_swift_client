package io.github.slacesa.simple_swift_client.test;




import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.slacesa.simpleSwiftClient.SimpleSwiftClient;
import io.github.slacesa.simpleSwiftClient.resources.SwiftConfig;
import io.github.slacesa.simpleSwiftClient.resources.SwiftFile;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Lunches a dummy web server and then performs authentication, and a few actions with storage.
 * @author SLC
 *
 */
@ExtendWith(VertxExtension.class)
public class SimpleSwiftClientTest {

	private static final Logger log = LoggerFactory.getLogger(SimpleSwiftClientTest.class);
	private static final String testConfigPath = "src/test/data/testconfig.json";
	
	private static HttpServer testServer;
	private static SimpleSwiftClient client;
	private static SwiftConfig config;
	
	@BeforeAll
	@DisplayName("Before")
	static void before(Vertx vertx, VertxTestContext testContext) throws Throwable {
		getTestConfig(vertx).compose(config ->
		SimpleSwiftTestServer.getTestServer(vertx, config).setHandler(thisServer -> {
			testServer = thisServer.result();
		}).compose(thisServer ->
		SimpleSwiftClient.retrieveClient(config).setHandler(thisClient -> {
			if(thisClient.succeeded()) {
				client = thisClient.result();
				testContext.completeNow();
			}
			else testContext.failNow(thisClient.cause());
			log.info("Client launched and connected: " + thisClient.succeeded());
		})));
	}

	private static Future<SwiftConfig> getTestConfig(Vertx vertx) {
		Promise<SwiftConfig> result = Promise.promise();
		if(config != null) {
			result.complete(config);
		}
		else {
			vertx.fileSystem().readFile(testConfigPath, ar -> {
				try {
					if(ar.succeeded()) {
						config = Json.decodeValue(ar.result(), SwiftConfig.class);
						result.complete(config);
					}
					else result.fail(ar.cause());
				}
				catch (Exception e) {
					result.fail(e.getCause());
				}
			});
		}
		return result.future();
	}

	@Test
	@DisplayName("getFileList")
	void getFileList(Vertx vertx, VertxTestContext testContext) throws Throwable {
		client.getFileList().setHandler(ar -> {
			if(ar.succeeded()) {
				if(ar.result() != null) {
					log.info("Test File list Size " + ar.result().size());
					for(SwiftFile file: ar.result())
						log.debug("* " + file.getName());
				}
				testContext.completeNow();
			}
			else {
				log.info(ar.cause().getMessage());
				testContext.failNow(ar.cause());
			}
		});
	}

	@Test
	@DisplayName("downloadFile")
	void downloadFile(Vertx vertx, VertxTestContext testContext) throws Throwable {
		Checkpoint notFound = testContext.checkpoint(1);
		Checkpoint sealed = testContext.checkpoint(1);
		Checkpoint success = testContext.checkpoint(1);

		client.downloadFile("notfound.txt").setHandler(ar ->{
			if(ar.succeeded()) testContext.failNow(new NoStackTraceThrowable("Expected to fail, file not found"));
			else if(ar.cause().getMessage().toLowerCase().contains("file not found")) 
				notFound.flag();
			else testContext.failNow(ar.cause());
		});

		client.downloadFile("sealed.txt").setHandler(ar ->{
			if(ar.succeeded()) testContext.failNow(new NoStackTraceThrowable("Expected to fail, file sealed"));
			else if(ar.cause().getMessage().toLowerCase().contains("try again")) 
				sealed.flag();
			else testContext.failNow(ar.cause());
		});
		
		client.downloadFile("existing.txt").setHandler(ar ->{
			if(ar.succeeded()) success.flag();
			else testContext.failNow(ar.cause());
		});
	}

	@Test
	@DisplayName("unsealFile")
	void unsealFile(Vertx vertx, VertxTestContext testContext) throws Throwable {
		Checkpoint notFound = testContext.checkpoint(1);
		Checkpoint sealed = testContext.checkpoint(1);
		Checkpoint success = testContext.checkpoint(1);

		client.unsealFile("notfound.txt").setHandler(ar ->{
			if(ar.succeeded()) testContext.failNow(new NoStackTraceThrowable("Expected to fail, file not found"));
			else if(ar.cause().getMessage().toLowerCase().contains("file not found")) 
				notFound.flag();
			else testContext.failNow(ar.cause());
		});

		client.unsealFile("sealed.txt").setHandler(ar ->{
			if(ar.succeeded() && ar.result() > 0) sealed.flag();
			else if(ar.succeeded()) testContext.failNow(new NoStackTraceThrowable("Expected to fail, file sealed"));
			else testContext.failNow(ar.cause());
		});
		
		client.unsealFile("existing.txt").setHandler(ar ->{
			if(ar.succeeded() && ar.result() == 0) success.flag();
			else testContext.failNow(ar.cause());
		});
	}

	@Test
	@DisplayName("uploadFile")
	void uploadFile(Vertx vertx, VertxTestContext testContext) throws Throwable {
		Checkpoint notFound = testContext.checkpoint(1);
		Checkpoint success = testContext.checkpoint(1);

		client.uploadFile(testConfigPath).setHandler(ar ->{
			if(ar.succeeded() && ar.result()) success.flag();
			else if(ar.succeeded()) testContext.failNow(new NoStackTraceThrowable("Not the desired status code"));
			else testContext.failNow(ar.cause());
		});

		client.uploadFile(testConfigPath+".").setHandler(ar ->{
			if(ar.succeeded() && ar.result()) testContext.failNow(new NoStackTraceThrowable("Expected to fail, file not found"));
			else if(ar.succeeded()) testContext.failNow(new NoStackTraceThrowable("Not the desired status code"));
			else notFound.flag();
		});
	}

	@Test
	@DisplayName("deleteFile")
	void deleteFile(Vertx vertx, VertxTestContext testContext) throws Throwable {
		Checkpoint notFound = testContext.checkpoint(1);
		Checkpoint success = testContext.checkpoint(1);

		client.deleteFile("notfound.txt").setHandler(ar ->{
			if(ar.succeeded() && ar.result()) testContext.failNow(new NoStackTraceThrowable("Expected to fail, file not found"));
			else if(ar.succeeded())
				notFound.flag();
			else testContext.failNow(ar.cause());
		});

		client.deleteFile("existing.txt").setHandler(ar ->{
			if(ar.succeeded()) success.flag();
			else testContext.failNow(ar.cause());
		});
	}

	@Test
	@DisplayName("backupFolder")
	void backupFolder(Vertx vertx, VertxTestContext testContext) throws Throwable {
		client.backupFolder("src/test/data", "testPassword").setHandler(ar -> {
			if(ar.succeeded()) testContext.completeNow();
			else testContext.failNow(ar.cause());
		});
	}

	@AfterAll
	@DisplayName("After")
	static void after(Vertx vertx, VertxTestContext testContext) throws Throwable {
		if(testServer != null) testServer.close();
		client.close();
		log.info("Exiting tests");
		testContext.completeNow();
	}
	
}
