package com.slacesa.zipper;

import java.io.File;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.NoStackTraceThrowable;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * Utility class to wrap an asynchronous zipper for Vert.X using zip4j
 * Needs to import net.lingala.zip4j 2.3.2
 * @see net.lingala.zip4j
 * @author SLC
 * @version 1.0
 */
public class Zipper {

	private ZipParameters params;
	private Vertx vertx;

	private static Zipper singleton;

	private Zipper(Vertx vertx) {
		params = new ZipParameters();
		params.setEncryptFiles(true);
		params.setEncryptionMethod(EncryptionMethod.AES);
		this.vertx = vertx;
	}

	public static Zipper getZipper(Vertx vertx) {
		if(vertx == null) return null;
		if(singleton == null)
			singleton = new Zipper(vertx);
		return singleton;
	}

	public Future<Void> zipFolder(String zipFileName, String sourceFolder, String password) {
		Promise<Void> result = Promise.promise();
		vertx.executeBlocking(
				promise -> {
					try {
						new ZipFile(zipFileName, password.toCharArray())
						.addFolder(new File(sourceFolder), params);
						promise.complete();
					}
					catch (ZipException ze) {
						promise.fail(ze.getCause());
					}
					catch (Exception e) {
						promise.fail(e.getCause());
					}
				},
				res -> {
					if(res.succeeded())result.complete();
					else result.fail(new NoStackTraceThrowable("Folder not found"));
				});
		return result.future();
    }
}
