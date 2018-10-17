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
 * @author jbuhacoff
 */
public class PostconfigureAttestationService extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureAttestationService.class);
    private SSH remote;

    public PostconfigureAttestationService(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {
        // we need to retrieve the mtwilson tls cert sha256 fingerprint
        // which is needed by other packages: tagent, director, kmsproxy

        // right now, just get it directly... if, in th future, we need to 
        // get multiple pieces of info, then maybe we would want to generate
        // a file on the remote server and then download that file with all of it.
        // NOTE: we need to specify the full path to the remote command
        String cmdGetTlsCertSha256 = "/usr/bin/sha256sum /opt/mtwilson/configuration/ssl.crt | /usr/bin/awk '{print $1}'";
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

            Result result = sshexec(client, cmdGetTlsCertSha256);
            String stdoutText = result.getStdout();

            // if the output looks like a valid sha256 digest, keep it:
            if (stdoutText != null) {
                String tlsCertSha256 = stdoutText.trim();
                if (Digest.sha256().isValidHex(tlsCertSha256)) {
                    setting("mtwilson.tls.cert.sha256", tlsCertSha256); // TODO: possibly rename this setting (and update any references to it) to be named similar to the new tls policy settings, since this is really a certificate-digest policy
                }
            }
            
            // now create a user that we will use to download the configuration data bundle later
            // ImportAttestationServiceCertificatesToKeyBroker needs the permission configuration_databundle:retrieve
            // ApproveKeyBrokerProxyUserInAttestationService needs the permissions users:search,user_login_certificates:search,user_login_certificates:retrieve,user_login_certificates:store
            String username = setting("mtwilson.quickstart.username");
            String password = setting("mtwilson.quickstart.password");
            // NOTE:  the mtwilson login-password command takes a SPACE-separated list of permissions, while the mtwilson-core password command (used in key broker and trust director) takes a COMMA-separated list of permissions 
            String cmdCreateQuickstartUser = "/opt/mtwilson/bin/mtwilson login-password " + username + " " + password + " --permissions configuration_databundle:retrieve users:search user_login_certificates:search user_login_certificates:retrieve user_login_certificates:store";
            Result createAdminUser = sshexec(client, cmdCreateQuickstartUser);
            if (createAdminUser.getExitCode() != 0) {
                log.error("Failed to create quickstart user in attestation service");
                fault(new Fault("Failed to create user"));
            }
            

        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

    }
}
