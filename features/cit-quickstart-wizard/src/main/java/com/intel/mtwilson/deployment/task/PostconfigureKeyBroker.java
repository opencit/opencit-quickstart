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
public class PostconfigureKeyBroker extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureKeyBroker.class);
    private SSH remote;

    public PostconfigureKeyBroker(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {

        // precondition:  providerName maps to a known providerClass
        String providerClass = setting("kms.key.provider");
        /*
         HashMap<String,String> providerMap = new HashMap<>();
         providerMap.put("kmip","com.intel.kms.kmip.client.KMIPKeyManager");
         providerMap.put("barbican","com.intel.kms.barbican.client.BarbicanKeyManager");
         String providerClass = providerMap.get(providerName);
         if( providerClass == null ) {
         fault(new Fault("Unknown key provider type: "+providerName));
         return;
         }
         */
        if (providerClass.isEmpty()) {
            fault(new Fault("Key provider not specified"));
            return;
        }

        // we need to retrieve the kms tls cert sha1 fingerprint
        // which is needed by other packages: director


        // right now, just get it directly... if, in th future, we need to 
        // get multiple pieces of info, then maybe we would want to generate
        // a file on the remote server and then download that file with all of it.
        // NOTE: we need to specify the full path to the remote command
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

            // get tls cert sha1 fingerprint
            String cmdGetTlsCertSha1 = "/bin/cat /opt/kms/configuration/https.properties | /bin/grep tls.cert.sha1 | /usr/bin/tr '=' ' ' | /usr/bin/awk '{print $2}'";
            Result getTlsCertSha1 = sshexec(client, cmdGetTlsCertSha1);

            // if the output looks like a valid sha1 digest, keep it:
            String stdoutText = getTlsCertSha1.getStdout();
            if (stdoutText != null) {
                String tlsCertSha1 = stdoutText.trim();
                if (Digest.sha1().isValidHex(tlsCertSha1)) {
                    setting("kms.tls.cert.sha1", tlsCertSha1); // TODO: possibly rename this setting (and update any references to it) to be named similar to the new tls policy settings, since this is really a certificate-digest policy
                }
            }

            // TODO: kms installer to support setting these variables in kms.env file, OR send these settings via the kms settings API
            remoteconfig(client, "endpoint.url", setting("kms.endpoint.keytransfer"));

            // configure the key provider ... kmip or barbican 
            remoteconfig(client, "key.manager.provider", providerClass);

            if (providerClass.equals("com.intel.kms.barbican.client.BarbicanKeyManager")) {
                remoteconfig(client, "barbican.project.id", setting("kms.barbican.project"));
                remoteconfig(client, "barbican.endpoint.url", setting("kms.barbican.url"));
            }
            if (providerClass.equals("com.intel.kms.kmip.client.KMIPKeyManager")) {
                remoteconfig(client, "kmip.targetHostname", setting("kms.kmip.url"));
            }

            // create the key broker admin user, following settings must be set in preconfigure task.
            String username = setting("kms.admin.username");
            String password = setting("kms.admin.password");
            String cmdCreateAdminUser = "/opt/kms/bin/kms.sh password " + username + " " + password + " --permissions *:*";
            Result createAdminUser = sshexec(client, cmdCreateAdminUser);
            if (createAdminUser.getExitCode() != 0) {
                log.error("Failed to create admin user in key broker");
                fault(new Fault("Failed to create user"));
            }
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

    }

    // ConnectionException, TransportException, IOException
    private void remoteconfig(SSHClientWrapper clientWrapper, String key, String value) throws Exception {
        Result result = sshexec(clientWrapper, "/opt/kms/bin/kms.sh config " + key + " " + value);
        if (result.getExitCode() != 0) {
            log.error("Failed to configure key broker with key: {} value: {}", key, value);
            fault(new Fault("Failed to set " + key));
        }
    }
}
