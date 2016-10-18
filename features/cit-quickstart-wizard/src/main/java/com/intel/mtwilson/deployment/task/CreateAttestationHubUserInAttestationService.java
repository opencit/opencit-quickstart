/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import java.util.UUID;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;

/**
 * This is an integration task: prior to installing AttestaionHub, a user must
 * be created in attestation service for AttestaionHub to access those APIs.
 *
 * @author soakx
 */
public class CreateAttestationHubUserInAttestationService extends AbstractPostconfigureTask {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(CreateAttestationHubUserInAttestationService.class);
	private SSH remote;

	/**
	 * The remote server to pass here is the Attestation Service, where we will
	 * ssh to create the director user.
	 * 
	 * @param remote
	 */
	public CreateAttestationHubUserInAttestationService(SSH remote) {
		this.remote = remote;
	}

	@Override
	public void execute() {

		/*
		 * TODO: generate random uuid after it, like kmsproxy does... and this
		 * should really be moved to director setup as a registration request w/
		 * mtwilson, so all we should have to do here is get that username from
		 * director and then go to mtwilson to approve it.
		 */
		String attestationHubUsername = setting("attestationhub.mtwilson.username");
		if (attestationHubUsername.isEmpty()) {
			attestationHubUsername = "attestationhub-" + UUID.randomUUID().toString();
			setting("attestationhub.mtwilson.username", attestationHubUsername);
		}
		String attestationHubPassword = setting("attestationhub.mtwilson.password");
		if (attestationHubPassword.isEmpty()) {
			int lengthBytes = 16;
			attestationHubPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
			setting("attestationhub.mtwilson.password", attestationHubPassword);
		}
		/*
		 * command to execute on attestation service to create the AttestaionHub
		 * user; TODO: if we can just call an API, that would be better than
		 * ssh+command. TODO: escape the AttestaionHub username and password
		 */
		String cmdCreateAttestaionHubUser = "/opt/mtwilson/bin/mtwilson login-password " + attestationHubUsername + " "
				+ attestationHubPassword + " --permissions *:*";

		try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

			sshexec(client, cmdCreateAttestaionHubUser);

		} catch (Exception e) {
			log.error("Connection failed", e);
			fault(new Connection(remote.getHost()));
		}

	}
}
