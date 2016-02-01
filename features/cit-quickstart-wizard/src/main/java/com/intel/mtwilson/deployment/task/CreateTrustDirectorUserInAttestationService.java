/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import java.util.UUID;

/**
 * This is an integration task: prior to installing trust director, a user must
 * be created in attestation service for trust director to access those APIs.
 *
 * @author jbuhacoff
 */
public class CreateTrustDirectorUserInAttestationService extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateTrustDirectorUserInAttestationService.class);
    private SSH remote;

    /**
     * The remote server to pass here is the Attestation Service,  where we 
     * will ssh to create the director user. 
     * @param remote 
     */
    public CreateTrustDirectorUserInAttestationService(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {

        String directorUsername = setting("director.mtwilson.username"); // TODO:  generate random uuid after it, like kmsproxy does... and this should really be moved to director setup as a registration request w/ mtwilson, so all we should have to do here is get that username from director and then go to mtwilson to approve it.
        if (directorUsername.isEmpty()) {
            directorUsername = "director-" + UUID.randomUUID().toString();
            setting("director.mtwilson.username", directorUsername);
        }
        String directorPassword = setting("director.mtwilson.password");
        if (directorPassword.isEmpty()) {
            int lengthBytes = 16;
            directorPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("director.mtwilson.password", directorPassword);
        }

        // command to execute on attestation service to create the trust director user;  TODO:  if we can just call an API, that would be better than ssh+command.
        // TODO:  escape the director username and password
        String cmdCreateTrustDirectorUser = "/opt/mtwilson/bin/mtwilson login-password " + directorUsername + " " + directorPassword + " --permissions trust_policies:certify";

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            
            sshexec(client, cmdCreateTrustDirectorUser);
            
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

        // postcondition:
        // DIRECTOR_MTWILSON_USERNAME and DIRECTOR_MTWILSON_PASSWORD must be set (and corresponding user actually created in mtwilson) 

    }
}
