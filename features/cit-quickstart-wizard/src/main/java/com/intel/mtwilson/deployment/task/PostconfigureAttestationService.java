/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;
import net.schmizz.sshj.connection.channel.direct.Session;

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
        // we need to retrieve the mtwilson tls cert sha1 fingerprint
        // which is needed by other packages: tagent, director, kmsproxy
        
        // right now, just get it directly... if, in th future, we need to 
        // get multiple pieces of info, then maybe we would want to generate
        // a file on the remote server and then download that file with all of it.
        // NOTE: we need to specify the full path to the remote command
        String cmdGetTlsCertSha1 = "/usr/bin/sha1sum /opt/mtwilson/configuration/ssl.crt | /usr/bin/awk '{print $1}'";
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();
            try(Session session = client.session()) {
                
                Result result = sshexec(session,cmdGetTlsCertSha1);
                String stdoutText = result.getStdout();
                
                // if the output looks like a valid sha1 digest, keep it:
                if( Digest.sha1().isValidHex(stdoutText) ) {
                    order.getSettings().put("mtwilson.tls.cert.sha1", stdoutText); // TODO: possibly rename this setting (and update any references to it) to be named similar to the new tls policy settings, since this is really a certificate-digest policy
                }
            
            }
            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }
        
    }
    
}
