/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;

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
        
        // precondition:  providerName maps to a known providerClass
        String providerName = order.getSettings().get("kms.key.provider");
        HashMap<String,String> providerMap = new HashMap<>();
        providerMap.put("kmip","com.intel.kms.kmip.client.KMIPKeyManager");
        providerMap.put("barbican","com.intel.kms.barbican.client.BarbicanKeyManager");
        String providerClass = providerMap.get(providerName);
        if( providerClass == null ) {
            fault(new Fault("Unknown key provider type: "+providerName));
            return;
        }

        // we need to retrieve the kms tls cert sha1 fingerprint
        // which is needed by other packages: director
        
        
        // right now, just get it directly... if, in th future, we need to 
        // get multiple pieces of info, then maybe we would want to generate
        // a file on the remote server and then download that file with all of it.
        // NOTE: we need to specify the full path to the remote command
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();
            try(Session session = client.session()) {

                // ensure output directory exists
                File outputDirectory = new File(Folders.repository("tasks") + File.separator + getId());
                outputDirectory.mkdirs();
                log.debug("Output directory: {}", outputDirectory.getAbsolutePath());

                // get tls cert sha1 fingerprint
                String cmdGetTlsCertSha1 = "/bin/cat /opt/kms/configuration/https.properties | /bin/grep kms.tls.cert.sha1 | /usr/bin/tr '=' ' ' | /usr/bin/awk '{print $2}'";
                Result getTlsCertSha1 = sshexec(session, cmdGetTlsCertSha1);
                
                // if the output looks like a valid sha1 digest, keep it:
                if( Digest.sha1().isValidHex(getTlsCertSha1.getStdout()) ) {
                    order.getSettings().put("kms.tls.cert.sha1", getTlsCertSha1.getStdout()); // TODO: possibly rename this setting (and update any references to it) to be named similar to the new tls policy settings, since this is really a certificate-digest policy
                }
                
                // TODO: kms installer to support setting these variables in kms.env file, OR send these settings via the kms settings API
                // configure the endpoint url to use in kms key responses; kms.port.http must be est in preconfigure task.
                String host = target.getHost();
                String port = order.getSettings().get("kms.port.http");
                remoteconfig(session, "endpoint.url", "http://"+host+":"+port);
                
                // configure the key provider ... kmip or barbican 
                remoteconfig(session, "key.manager.provider",providerClass);
                
                if( providerName.equals("com.intel.kms.barbican.client.BarbicanKeyManager") ) {
                    remoteconfig(session, "barbican.project.id", order.getSettings().get("kms.barbican.project"));
                    remoteconfig(session, "barbican.endpoint.url", order.getSettings().get("kms.barbican.url"));
                }
                if( providerName.equals("com.intel.kms.kmip.client.KMIPKeyManager") ) {
                    remoteconfig(session, "kmip.targetHostname", order.getSettings().get("kms.kmip.url"));
                }
                
                // create the key broker admin user, following settings must be set in preconfigure task.
                String username = order.getSettings().get("kms.admin.username");
                String password = order.getSettings().get("kms.admin.password");
                String cmdCreateAdminUser = "/opt/kms/bin/kms password "+username+" "+password+" --permissions *:*";
                Result createAdminUser = sshexec(session, cmdCreateAdminUser);
                if( createAdminUser.getExitCode() != 0 ) {
                    log.error("Failed to create admin user in key broker");
                    fault(new Fault("Failed to create user"));
                }
            }
            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }
        
    }
        
    private void remoteconfig(Session session, String key, String value) throws ConnectionException, TransportException, IOException {
        Result result = sshexec(session, "/opt/kms/bin/kms config "+key+" "+value);
        if( result.getExitCode() != 0 ) {
            log.error("Failed to configure key broker with key: {} value: {}", key, value);
            fault(new Fault("Failed to set "+key));
        }
    }
}
