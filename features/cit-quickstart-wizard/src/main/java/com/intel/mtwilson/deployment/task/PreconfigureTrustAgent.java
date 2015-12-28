/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Generates trustagent.env using a template. 
 * 
 * @author jbuhacoff
 */
public class PreconfigureTrustAgent extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureTrustAgent.class);
    private List<FileTransferDescriptor> manifest;
    private File envFile;
    
    /**
     * Initializes the task with a file transfer manifest; the file(s) mentioned
     * in the manifest will not be available until AFTER execute() completes
     * successfully.
     */
    public PreconfigureTrustAgent() {
        super(); // initializes taskDirectory
        envFile = new File(taskDirectory.getAbsolutePath() + File.separator + "trustagent.env");
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }
    
    @Override
    public void execute() {
        // preconditions:  
        // MTWILSON_HOST, MTWILSON_PORT, and MTWILSON_TLS_CERT_SHA1 must be set ;  note that if using a load balanced mtwilson, the tls cert is for the load balancer
        // the host and port are set by PreconfigureAttestationService, but the tls sha1 fingerprint is set by PostconfigureAttestationService.
        // either way, the sync task forces all attestation service tasks to complete before key broker proxy tasks start, so these settings should be present.
        if( !order.getSettings().containsKey("mtwilson.host") || !order.getSettings().containsKey("mtwilson.port.https")|| !order.getSettings().containsKey("mtwilson.tls.cert.sha1") ) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }
        // the PreconfigureAttestationService task must already be executed 
        data.put("MTWILSON_HOST", order.getSettings().get("mtwilson.host"));
        data.put("MTWILSON_PORT", order.getSettings().get("mtwilson.port.https"));
        // the PostconfigureAttestationService task must already be executed 
        data.put("MTWILSON_TLS_CERT_SHA1", order.getSettings().get("mtwilson.tls.cert.sha1"));

        // preconditions:
        // TRUSTAGENT_MTWILSON_USERNAME and TRUSTAGENT_MTWILSON_PASSWORD must be set,  these are created by CreateTrustAgentUserInAttestationService.... 
        data.put("TRUSTAGENT_MTWILSON_USERNAME", order.getSettings().get("trustagent.mtwilson.username"));
        data.put("TRUSTAGENT_MTWILSON_PASSWORD", order.getSettings().get("trustagent.mtwilson.password"));
        
        
        // trustagent settings:  TODO:  looks like env file doesn't include customziing the trustagent port
        port();
        data.put("JETTY_PORT", order.getSettings().get("trustagent.port.http"));
        data.put("JETTY_SECURE_PORT", order.getSettings().get("trustagent.port.https"));
        
        // optional:
        // IF key broker proxy is installed, then we need its settings 
        data.put("KMSPROXY_HOST", order.getSettings().get("kmsproxy.host"));
        data.put("KMSPROXY_PORT", order.getSettings().get("kmsproxy.port.http"));  // NOTE:  when trustagent uses key broker proxy, it uses http not https ; see CIT 3.0 architecture
        
        
        data.put("TRUSTAGENT_HOST", target.getHost());
        
        // generate the .env file using pre-configuration data
        render("trustagent.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (!order.getSettings().containsKey("trustagent.port.http") || !order.getSettings().containsKey("trustagent.port.https")) {
            // TODO:  the port conflict check for trustagent should not be based on how many packages WE are installing... because tehre may be already be other software on that node;  that's why the default is 1443 already.
            if (target.getPackages().size() == 1) {
                order.getSettings().put("trustagent.port.http", "1081");  // the default trustagent http port, not known to be used by any common software
                order.getSettings().put("trustagent.port.https", "1443"); // the default trustagent https port, not known to be used by any common software
            } else {
                order.getSettings().put("trustagent.port.http", "17080");
                order.getSettings().put("trustagent.port.https", "17443");
            }
        }
    }


    @Override
    public String getPackageName() {
        return "trust_agent";
    }

    /**
     * Must be called AFTER execute() to get list of files that should be
     * transferred to the remote host
     * @return 
     */
    @Override
    public List<FileTransferDescriptor> getFileTransferManifest() {
        return manifest;
    }

    
}
