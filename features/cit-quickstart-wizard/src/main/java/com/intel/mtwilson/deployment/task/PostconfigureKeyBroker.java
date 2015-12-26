/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author jbuhacoff
 */
public class PostconfigureKeyBroker extends AbstractPostconfigureTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureKeyBroker.class);
    private SSH remote;

    public PostconfigureKeyBroker(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {
        // we need to retrieve the kms tls cert sha1 fingerprint
        // which is needed by other packages: director
        
        // right now, just get it directly... if, in th future, we need to 
        // get multiple pieces of info, then maybe we would want to generate
        // a file on the remote server and then download that file with all of it.
        // NOTE: we need to specify the full path to the remote command
        String cmdGetTlsCertSha1 = "/bin/cat /opt/kms/configuration/https.properties | /bin/grep kms.tls.cert.sha1 | /usr/bin/tr '=' ' ' | /usr/bin/awk '{print $2}'";
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();
            try(Session session = client.session()) {
                Session.Command command = session.exec(cmdGetTlsCertSha1);
                InputStream stdout = command.getInputStream();
                InputStream stderr = command.getErrorStream();
                String stdoutText = IOUtils.toString(stdout, "UTF-8");
                
                // if the output looks like a valid sha1 digest, keep it:
                if( Digest.sha1().isValidHex(stdoutText) ) {
                    order.getSettings().put("kms.tls.cert.sha1", stdoutText); // TODO: possibly rename this setting (and update any references to it) to be named similar to the new tls policy settings, since this is really a certificate-digest policy
                }
                
                // REST OF THIS SECTION IS JUST TO RECORD THE OUTPUT FOR DEBUGGING
                
                String stderrText = IOUtils.toString(stderr, "UTF-8");
                log.debug("result: {}", stdoutText);

                // ensure output directory exists
                File outputDirectory = new File(Folders.repository("tasks") + File.separator + getId());
                outputDirectory.mkdirs();
                log.debug("Output directory: {}", outputDirectory.getAbsolutePath());
                
                // store the stdout into a file
                File stdoutFile = new File(Folders.repository("tasks") + File.separator + getId() + File.separator + "stdout.log");                
                FileUtils.writeStringToFile(stdoutFile, stdoutText, Charset.forName("UTF-8"));
                
                // store the stderr into a file
                File stderrFile = new File(Folders.repository("tasks") + File.separator + getId() + File.separator + "stderr.log");                
                FileUtils.writeStringToFile(stderrFile, stderrText, Charset.forName("UTF-8"));
            
            }
            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }
        
    }
    
}
