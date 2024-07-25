package io.github.slacesa.simpleSwiftClient;

import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import io.github.slacesa.simpleSwiftClient.resources.AuthMessage;
import io.github.slacesa.simpleSwiftClient.resources.SwiftConfig;
import io.github.slacesa.simpleSwiftClient.resources.SwiftFile;
import io.github.slacesa.zipper.Zipper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.uritemplate.UriTemplate;

/**
 * Simple Swift Client, used to connect to authenticate, upload, list and download files from cold storage
 * typical usage for upload is: retrieveClient() to get a valid client once and then uploadFile() every time you need it.
 * @author SLC
 * @version 1.0
 */
public class SimpleSwiftClient {

	private SwiftConfig config;
	private String token;
	private DateTime token_expires;
	private Vertx vertx;
	private WebClient webclient;
	private Zipper zipper;

	private static SimpleSwiftClient swiftClient;
	
	private static final Logger log = Logger.getLogger(SimpleSwiftClient.class.getName());

	/**
	 * Retrieve a singleton simple Swift Client, if already initialized it overwrites previous configuration
	 * @param config the required config
	 * @return a future to a simple swift client
	 */
	public static Future<SimpleSwiftClient> retrieveClient(SwiftConfig config) {
		Promise<SimpleSwiftClient> result = Promise.promise();
		boolean force = (swiftClient != null);
		if(force) swiftClient.config = config;
		else swiftClient = new SimpleSwiftClient(config);

		swiftClient.retrieveToken(force).onComplete(ar -> {
			if(ar.succeeded())
				result.complete(swiftClient);
			else
				result.fail(ar.cause());
		});
		return result.future();
	}

	/**
	 * Retrieve the singleton simple Swift Client, if already initialized, fails otherwise
	 * @return a future to a simple swift client
	 */
	public static Future<SimpleSwiftClient> retrieveClient() {
		Promise<SimpleSwiftClient> result = Promise.promise();		
		if(swiftClient != null) result.complete(swiftClient);
		else result.fail(new NullPointerException("The client has not been initialized"));
		return result.future();
	}

	/**
	 * Retrieves a list of current files, including information on availability (policy_retrieval_state [sealed, unsealing, unsealed] and policy_retrieval_delay)
	 * @return an array containing a list of files
	 */
	public Future<List<SwiftFile>> getFileList() {
		Promise<List<SwiftFile>> result = Promise.promise();
		getter(null).onComplete(response -> {
			try {
				if(response.succeeded() && response.result().statusCode() == 200)
					result.complete(Arrays.asList(Json.decodeValue(response.result().bodyAsBuffer(), SwiftFile[].class)));
				else if(response.succeeded() && response.result().statusCode() == 404)
					result.fail(new FileNotFoundException("Container not found"));
			}
			catch (Exception e) {
				result.fail(e.getCause());
			}
		});
		return result.future();
	}

	/**
	 * Unseals a file, retrieves the time when it will be ready
	 * @param filename the file name to unseal
	 * @return a future time (in seconds) when the file will be ready
	 */
	public Future<Integer> unsealFile(String filename) {
		Promise<Integer> result = Promise.promise();
		getter(filename).onComplete(response -> {
			if(response.succeeded()) {
				if(response.result().statusCode() == 429)
					result.complete(Integer.valueOf(response.result().getHeader("Retry-After")));
				else if(response.result().statusCode() == 200)
					result.complete(0);
				else if(response.result().statusCode() == 404)
					result.fail(new FileNotFoundException("File not found: " + filename));
				else result.fail(new Exception("Unknown status code: " + response.result().statusCode()));
			}
			else result.fail(response.cause());
		});
		return result.future();
	}

	/**
	 * Retrieves a buffer to a file (if the file is unsealed)
	 * @param filename the file name to retrieve
	 * @return a future buffer to the file
	 */
	public Future<Buffer> downloadFile(String filename) {
		Promise<Buffer> result = Promise.promise();
		getter(filename).onComplete(response -> {
			if(response.succeeded()) {
				if(response.result().statusCode() == 200)
					result.complete(response.result().bodyAsBuffer());
				else if(response.result().statusCode() == 429)
					result.fail("Not ready, try again in " + response.result().getHeader("Retry-After"));
				else if(response.result().statusCode() == 404)
					result.fail(new FileNotFoundException("File not found: " + filename));
				else result.fail(new Exception("Unknown status code: " + response.result().statusCode()));
			}
			else result.fail(response.cause());
		});
		return result.future();
	}

	/**
	 * Reads a file from disk and uploads it to default folder
	 * TODO Maybe allow to choose target folder
	 * @param filename the source file name
	 * @return a future boolean to result
	 */
	public Future<Boolean> uploadFile(String filename) {
		Promise<Boolean> result = Promise.promise();
		localReadFile(filename).compose(fileContent -> 
		putter(filename, fileContent).onComplete(isSent ->{
			result.complete(isSent.succeeded() && isSent.result());
		})).otherwise(t1 -> {
			result.fail(new NoStackTraceThrowable("File not found"));
			return null;
		});
		return result.future();
	}

	/**
	 * Uploads a file with target name and target content and stores it to default folder
	 * TODO Maybe allow to choose target folder
	 * @param filename the source file name
	 * @param fileContent the data the new file will contain
	 * @return a future boolean to result
	 */
	public Future<Boolean> uploadFile(String filename, Buffer fileContent) {
		Promise<Boolean> result = Promise.promise(); 
		putter(filename, fileContent).onComplete(isSent ->{
			result.complete(isSent.succeeded() && isSent.result());
		});
		return result.future();
	}

	/**
	 * Deletes a file
	 * @param filename the file name to delete
	 * @return a future true if the file was deleted
	 */
	public Future<Boolean> deleteFile(String filename) {
		Promise<Boolean> result = Promise.promise();
		deleter(filename).onComplete(isDeleted ->{
			result.complete(isDeleted.succeeded() && isDeleted.result());
		});
		return result.future();
	}

	/**
	 * Used to backup a folder on our remote swift container using a password protected zip file
	 * @param folderPath the source file, the last part will be used to generate the zip file name (e.g. ancestor/parent/child becomes child.zip)
	 * @param password the password used to protect the zip file
	 * @return a void promise, successful if the folder was zipped, sent and the temp zip was deleted 
	 */
	public Future<Void> backupFolder(String folderPath, String password) {
		Promise<Void> result = Promise.promise();
		String[] folders = folderPath.split("/");
		String zipFileName = folders[folders.length-1]+".zip";
		zipper.zipFolder(zipFileName, folderPath, password).compose(v ->
		uploadFile(zipFileName).compose(isSent ->
		localDeleteFile(zipFileName).onComplete(v2 -> {
			if(v2.succeeded()) result.complete();
		}))).otherwise(err -> {
			result.fail(err.getCause());
			return null;
		});
		return result.future();
	}

	/**
	 * Closes the client and frees all resources
	 */
	public void close() {
		webclient.close();
	}

	/**
	 * A minimal Swift Client
	 * @param username a Keystone user, with storage rights
	 * @param password a Keystone password
	 */
	private SimpleSwiftClient(SwiftConfig config) {
		this.config = config;
		this.vertx = Vertx.currentContext().owner();
		this.webclient = WebClient.create(vertx);
		this.zipper = Zipper.getZipper(vertx);
	}

	/**
	 * Retrieve a valid token
	 * Checks if there is a current valid token, otherwise it retrieves one
	 * @param force ignores the validity check, retrieves a new token
	 * @return a future to a valid token
	 */
	private Future<String> retrieveToken(boolean force) {
		Promise<String> result = Promise.promise();
		if(!force && token != null && token_expires != null && token_expires.isAfterNow()) {
			result.complete(token);
		}
		else {
			JsonObject authMessage = new AuthMessage(config.getUsername(), config.getPassword()).parse();
			webclient.post(
					config.getPort(),
					config.getAuth_host(),
					config.getAuth_endpoint())
			.ssl(config.getPort()==443)
			.putHeader("Content-Type", "application/json")
			.sendJsonObject(authMessage, ar -> {
				if(ar.succeeded() && (ar.result().statusCode() == 200 || ar.result().statusCode() == 201)) {
					token = ar.result().getHeader("X-Subject-Token");
					log.fine("Token " + token);
					JsonObject bodyResponse = ar.result().bodyAsJsonObject();
					token_expires = DateTime.parse(bodyResponse.getJsonObject("token").getString("expires_at"));
					// Sets a timer to refresh token one hour before expiration
					vertx.setTimer(token_expires.minusHours(1).getMillis()-DateTime.now().getMillis(), timer -> {
						retrieveToken(true);
					});
					result.complete(token);
				}
				else {
					//Fail, but retry after 1 minute
					token = null;
					vertx.setTimer(60000, timer -> {
						retrieveToken(true);
					});
					//log.info(String.valueOf(ar.result().statusCode()));
					//log.info(ar.result().bodyAsString());
					result.fail(ar.succeeded()?new NoStackTraceThrowable("Status Code not 200 OK"):ar.cause());
				}
			});
		}
		return result.future();
	}

	/**
	 * Multi purpose getter: can request list of files (filename == null), unseal a file (status code 429) or retrieve a file (status code 200), depending on circustances
	 * @return an HttpResponse with the result
	 */
	private Future<HttpResponse<Buffer>> getter(String filename) {
		Promise<HttpResponse<Buffer>> result = Promise.promise();
		String extra = (filename == null)? "?policy_extra=true" : "/" + filename;
		webclient.get(
				config.getPort(),
				config.getStorage_host(),
				config.getStorage_endpoint()+extra)
		.ssl(config.getPort()==443)
		.putHeader("Accept", "application/json")
		.putHeader("X-Auth-Token", token)
		.send(response -> {
			if(response.succeeded())
				result.complete(response.result());
			else result.fail(response.cause());
		});
		return result.future();
	}

	/**
	 * Multi purpose deleter: can delete a container if empty (filename == null) or delete a file (returns 204 when successful)
	 * @return an HttpResponse with the result
	 */
	private Future<Boolean> deleter(String filename) {
		Promise<Boolean> result = Promise.promise();
		webclient.delete(
				config.getPort(),
				config.getStorage_host(),
				config.getStorage_endpoint()+"/"+filename)
		.ssl(config.getPort()==443)
		.putHeader("Accept", "application/json")
		.putHeader("X-Auth-Token", token)
		.send(response -> {
			if(response.succeeded())
				result.complete(response.result().statusCode() == 204);
			else result.fail(response.cause());
		});
		return result.future();
	}

	/**
	 * Reads target file
	 * @param filename the file name
	 * @return a future data buffer to target file
	 */
	private Future<Buffer> localReadFile(String filename) {
		Promise<Buffer> result = Promise.promise();
		vertx.fileSystem().readFile(filename, ar -> {
			if(ar.succeeded()) result.complete(ar.result());
			else result.fail(ar.cause());
//			log.debug("File length " + ar.result().length());
		});
		return result.future();
	}

	/**
	 * Deletes target file
	 * @param filename the file name
	 * @return a void future
	 */
	private Future<Void> localDeleteFile(String filename) {
		Promise<Void> result = Promise.promise();
		vertx.fileSystem().delete(filename, ar -> {
			if(ar.succeeded()) result.complete();
			else result.fail(ar.cause());
		});
		return result.future();
	}

	/**
	 * Puts a file names filename to default folder containing given fileContent
	 * TODO Maybe allow to choose target folder
	 * @param filename the file name
	 * @param fileContent the data buffer to write
	 * @return a future boolean to result
	 */
	private Future<Boolean> putter(String filename, Buffer fileContent) {
		Promise<Boolean> result = Promise.promise();
		webclient.put(
				config.getPort(),
				config.getStorage_host(),
				UriTemplate.of(config.getStorage_endpoint()+"/"+filename))
		.ssl(config.getPort()==443)
		.putHeader("X-Storage-Policy", "PCA")
		.putHeader("X-Auth-Token", token)
		.putHeader("Content-Length", Integer.toString(fileContent.length()))
		.putHeader("Etag", computeMD5(fileContent))
		.sendBuffer(fileContent, ar -> {
			result.complete(ar.succeeded() && ar.result().statusCode() == 201);
			if(!ar.succeeded()) result.fail(ar.cause());
		});
		return result.future();
	}

	private final static char[] hexArray = "0123456789abcdef".toCharArray();

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private String computeMD5(Buffer fileContent) {
		String result = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			result = bytesToHex(md.digest(fileContent.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return result;
	}
}
