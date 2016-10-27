/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;

/**
 *
 * @author soakx
 */
public class PreconfigureAttestationHub extends AbstractPreconfigureTask implements FileTransferManifestProvider {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureAttestationHub.class);
	private List<FileTransferDescriptor> manifest;
	private File envFile;

	/**
	 * Initializes the task with a file transfer manifest; the file(s) mentioned
	 * in the manifest will not be available until AFTER execute() completes
	 * successfully.
	 */
	public PreconfigureAttestationHub() {
		super(); // initializes taskDirectory
		envFile = getTaskDirectory().toPath().resolve("attestation-hub.env").toFile();
		manifest = new ArrayList<>();
		manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
	}

	@Override
	public void execute() {
		/*
		 * preconditions: MTWILSON_HOST, MTWILSON_PORT, and
		 * MTWILSON_TLS_CERT_SHA1 must be set ; note that if using a load
		 * balanced mtwilson, the tls cert is for the load balancer the host and
		 * port are set by PreconfigureAttestationService, but the tls sha1
		 * fingerprint is set by PostconfigureAttestationService. either way,
		 * the sync task forces all attestation service tasks to complete before
		 * key broker proxy tasks start, so these settings should be present.
		 */
	/*	setting("mtwilson.host", "10.35.35.175");  
		setting("mtwilson.port.https", "8443");
		setting("mtwilson.tls.cert.sha1", "32120f52f8bcbb59590688fcb52118def1d3ce3d");*/
		if (setting("mtwilson.host").isEmpty() || setting("mtwilson.port.https").isEmpty()
				|| setting("mtwilson.tls.cert.sha1").isEmpty()) {
			log.debug("mtwilson.host = {}", setting("mtwilson.host"));
			log.debug("mtwilson.port.https = {}", setting("mtwilson.port.https"));
			log.debug("mtwilson.tls.cert.sha1 = {}", setting("mtwilson.tls.cert.sha1"));
			throw new IllegalStateException("Missing required settings"); // TODO:
																			// rewrite
																			// as
																			// a
																			// precondition
		}

		// precondition:
		// DIRECTOR_MTWILSON_USERNAME and DIRECTOR_MTWILSON_PASSWORD must be set
		// (and corresponding user actually created in mtwilson)
		// these are done by a separate integration task that must run before
		// this one
/*		setting("attestationhub.mtwilson.username", "admin");
		setting("attestationhub.mtwilson.password", "password");*/
		if (setting("attestationhub.mtwilson.username").isEmpty()
				|| setting("attestationhub.mtwilson.password").isEmpty()) {
			throw new IllegalStateException("Missing required settings");
		}

		setting("attestationhub.host", target.getHost());

		port();

		String endpointUrl = setting("attestationhub.endpoint.url");
		if (endpointUrl.isEmpty()) {
			setting("attestationhub.endpoint.url",
					"https://" + setting("attestationhub.host") + ":" + setting("attestationhub.port.https"));
		}

		data.put("ATTESTATION_HUB_PORT_HTTP", setting("attestationhub.port.http"));
		data.put("ATTESTATION_HUB_PORT_HTTPS", setting("attestationhub.port.https"));

		// make these global settings available to attestationhub.env.st4
		// template
		data.put("MTWILSON_HOST", setting("mtwilson.host"));
		data.put("MTWILSON_PORT", setting("mtwilson.port.https"));
		data.put("MTWILSON_TLS_CERT_SHA1", setting("mtwilson.tls.cert.sha1"));
		data.put("ATTESTATION_HUB_MTWILSON_USERNAME", setting("attestationhub.mtwilson.username"));
		data.put("ATTESTATION_HUB_MTWILSON_PASSWORD", setting("attestationhub.mtwilson.password"));

		// TODO: generate ATTESTATION_HUB_DB_USERNAME, ATTESTATION_HUB_DB_PASSWORD

		// the admin username and password are generated here and stored in
		// settings
		// but not added to .env file because key broker .env does not support
		// creating the admin user that way. so we create it later in the
		// postconfigure task.
		String username = setting("attestationhub.admin.username");
		if (username.isEmpty()) {
			username = "admin";
			setting("attestationhub.admin.username", username);
		}
		String password = setting("attestationhub.admin.password");
		if (password.isEmpty()) {
			int lengthBytes = 16;
			password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
			setting("attestationhub.admin.password", password);
		}
		data.put("ATTESTATION_HUB_ADMIN_USERNAME", username);
		data.put("ATTESTATION_HUB_ADMIN_PASSWORD", password);

		String dbUsername = setting("attestationhub.database.username");
		if (dbUsername.isEmpty()) {
			dbUsername = "admin";
			setting("attestationhub.database.username", dbUsername);
		}
		String dbPassword = setting("attestationhub.database.password");
		if (dbPassword.isEmpty()) {
			int lengthBytes = 16;
			dbPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
			setting("attestationhub.database.password", dbPassword);
		}
		data.put("ATTESTATION_HUB_DATABASE_USERNAME", dbUsername);
		data.put("ATTESTATION_HUB_DATABASE_PASSWORD", dbPassword);
		data.put("ATTESTATION_HUB_POLL_INTERVAL", setting("attestationhub.poll.interval"));
		data.put("ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH", setting("attestationhub.tenant.configuration"));
		// generate the .env file using pre-configuration data
		render("attestationhub.env.st4", envFile);
	}

	private void port() {
		// if the target has more than one software package to be installed on
		// it,
		// use our alternate port
		if (setting("attestationhub.port.http").isEmpty() || setting("attestationhub.port.https").isEmpty()) {
			if (target.getPackages().size() == 1) {
				setting("attestationhub.port.http", "82");
				setting("attestationhub.port.https", "445");
			} else {
				setting("attestationhub.port.http", "19082");
				setting("attestationhub.port.https", "19445");
			}
		}
	}

	@Override
	public String getPackageName() {
		return "attestation_hub";
	}

	/**
	 * Must be called AFTER execute() to get list of files that should be
	 * transferred to the remote host
	 *
	 * @return
	 */
	@Override
	public List<FileTransferDescriptor> getFileTransferManifest() {
		return manifest;
	}
}
