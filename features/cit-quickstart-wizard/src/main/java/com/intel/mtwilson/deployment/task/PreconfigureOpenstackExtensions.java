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
 * Generates mtwilson-openstack-controller.env using a template. 
 * 
 * @author jbuhacoff
 */
public class PreconfigureOpenstackExtensions extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureOpenstackExtensions.class);
    private List<FileTransferDescriptor> manifest;
    private File envFile;
    
    /**
     * Initializes the task with a file transfer manifest; the file(s) mentioned
     * in the manifest will not be available until AFTER execute() completes
     * successfully.
     */
    public PreconfigureOpenstackExtensions() {
        super(); // initializes taskDirectory
        envFile = new File(taskDirectory.getAbsolutePath() + File.separator + "mtwilson-openstack-controller.env");
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }
    
    @Override
    public void execute() {
        // preconditions:  
        // MTWILSON_HOST, MTWILSON_PORT, and MTWILSON_TLS_CERT_SHA1 must be set ;  note that if using a load balanced mtwilson, the tls cert is for the load balancer
        // the host and port are set by PreconfigureAttestationService, but the tls sha1 fingerprint is set by PostconfigureAttestationService.
        // either way, the sync task forces all attestation service tasks to complete before key broker proxy tasks start, so these settings should be present.
        if( !order.getSettings().containsKey("mtwilson.host") || !order.getSettings().containsKey("mtwilson.port.https")|| !order.getSettings().containsKey("mtwilson.tls.cert.sha1")|| !order.getSettings().containsKey("mtwilson.tag.admin.username")|| !order.getSettings().containsKey("mtwilson.tag.admin.password") ) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }
        
        // the PreconfigureAttestationService task must already be executed 
        data.put("MTWILSON_HOST", order.getSettings().get("mtwilson.host"));
        data.put("MTWILSON_PORT", order.getSettings().get("mtwilson.port.https"));
        data.put("MTWILSON_TAG_ADMIN_USERNAME", order.getSettings().get("mtwilson.tag.admin.username"));
        data.put("MTWILSON_TAG_ADMIN_PASSWORD", order.getSettings().get("mtwilson.tag.admin.username"));
        // the PostconfigureAttestationService task must already be executed 
        data.put("MTWILSON_TLS_CERT_SHA1", order.getSettings().get("mtwilson.tls.cert.sha1"));
        // generate the .env file using pre-configuration data
        render("mtwilson-openstack-controller.env.st4", envFile);
    }


    @Override
    public String getPackageName() {
        return "openstack_extensions";
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
