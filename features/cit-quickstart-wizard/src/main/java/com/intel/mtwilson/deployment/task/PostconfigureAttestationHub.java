/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;

/**
 *
 * @author soakx
 */
public class PostconfigureAttestationHub extends AbstractPostconfigureTask {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureAttestationHub.class);
	private SSH remote;

	public PostconfigureAttestationHub(SSH remote) {
		this.remote = remote;
	}

	@Override
	public void execute() {

		try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

			// get tls cert sha256 fingerprint
			String cmdGetTlsCertSha256 = "/bin/cat /opt/attestation-hub/configuration/https.properties | /bin/grep tls.cert.sha256 | /usr/bin/tr '=' ' ' | /usr/bin/awk '{print $2}'";
			Result getTlsCertSha256 = sshexec(client, cmdGetTlsCertSha256);

			// if the output looks like a valid sha256 digest, keep it:
			String stdoutText = getTlsCertSha256.getStdout();
			if (stdoutText != null) {
				String tlsCertSha256 = stdoutText.trim();
				if (Digest.sha256().isValidHex(tlsCertSha256)) {
					setting("attestationhub.tls.cert.sha256", tlsCertSha256); // TODO:
																				// possibly
																				// rename
																				// this
																				// setting
																				// (and
																				// update
																				// any
																				// references
																				// to
																				// it)
																				// to
																				// be
																				// named
																				// similar
																				// to
																				// the
																				// new
																				// tls
																				// policy
																				// settings,
																				// since
																				// this
																				// is
																				// really
																				// a
																				// certificate-digest
																				// policy
				}
			}

			// create the attestation hub admin user, following settings must be
			// set in preconfigure task.
			// for the hub, the API is the only interface
			String username = setting("attestationhub.admin.username");
			String password = setting("attestationhub.admin.password");
			String cmdCreateAdminUser = "/opt/attestation-hub/bin/attestation-hub.sh password " + username + " "
					+ password + " --permissions *:*";
			Result createAdminUser = sshexec(client, cmdCreateAdminUser);
			if (createAdminUser.getExitCode() != 0) {
				log.error("Failed to create admin user in attestation hub");
				fault(new Fault("Failed to create user"));
			}
		} catch (Exception e) {
			log.error("Connection failed", e);
			fault(new Connection(remote.getHost()));
		}

	}

}
