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
public class PostconfigureTrustDirector extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureTrustDirector.class);
    private SSH remote;

    public PostconfigureTrustDirector(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

            // get tls cert sha1 fingerprint
            String cmdGetTlsCertSha1 = "/bin/cat /opt/director/configuration/https.properties | /bin/grep tls.cert.sha1 | /usr/bin/tr '=' ' ' | /usr/bin/awk '{print $2}'";
            Result getTlsCertSha1 = sshexec(client, cmdGetTlsCertSha1);

            // if the output looks like a valid sha1 digest, keep it:
            String stdoutText = getTlsCertSha1.getStdout();
            if (stdoutText != null) {
                String tlsCertSha1 = stdoutText.trim();
                if (Digest.sha1().isValidHex(tlsCertSha1)) {
                    setting("director.tls.cert.sha1", tlsCertSha1); // TODO: possibly rename this setting (and update any references to it) to be named similar to the new tls policy settings, since this is really a certificate-digest policy
                }
            }

            // create the key broker admin user, following settings must be set in preconfigure task.
            String username = setting("director.admin.username");
            String password = setting("director.admin.password");
            String cmdCreateAdminUser = "/opt/director/bin/director.sh password " + username + " " + password + " --permissions *:*";
            Result createAdminUser = sshexec(client, cmdCreateAdminUser);
            if (createAdminUser.getExitCode() != 0) {
                log.error("Failed to create admin user in trust director");
                fault(new Fault("Failed to create user"));
            }
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

    }

}
