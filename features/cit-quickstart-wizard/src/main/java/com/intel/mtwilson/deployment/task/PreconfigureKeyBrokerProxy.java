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
 * Generates kmsproxy.env using a template. 
 * 
 * @author jbuhacoff
 */
public class PreconfigureKeyBrokerProxy extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureKeyBrokerProxy.class);
    private List<FileTransferDescriptor> manifest;

    public PreconfigureKeyBrokerProxy() {
        super();
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
        
        // key broker proxy settings
        port();
        data.put("JETTY_PORT", order.getSettings().get("kmsproxy.port.http"));
        data.put("JETTY_SECURE_PORT", order.getSettings().get("kmsproxy.port.https"));
        
        // the PreconfigureAttestationService task must already be executed 
        data.put("MTWILSON_HOST", order.getSettings().get("mtwilson.host"));
        data.put("MTWILSON_PORT", order.getSettings().get("mtwilson.port.https"));
        // the PostconfigureAttestationService task must already be executed 
        data.put("MTWILSON_TLS_CERT_SHA1", order.getSettings().get("mtwilson.tls.cert.sha1"));
        // generate the .env file using pre-configuration data
        File envFile = render("kmsproxy.env.st4", "kmsproxy.env");
        // collect all pre-configuration data...
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, "kmsproxy.env"));
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (!order.getSettings().containsKey("kmsproxy.port.http") || !order.getSettings().containsKey("kmsproxy.port.https")) {
            if (target.getPackages().size() == 1) {
                order.getSettings().put("kmsproxy.port.http", "80");
                order.getSettings().put("kmsproxy.port.https", "443");
            } else {
                order.getSettings().put("kmsproxy.port.http", "21080");
                order.getSettings().put("kmsproxy.port.https", "21443");
            }
        }
    }


    @Override
    public String getPackageName() {
        return "key_broker_proxy";
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
