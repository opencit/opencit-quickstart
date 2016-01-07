/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.RandomUtil;
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
        envFile = getTaskDirectory().toPath().resolve("mtwilson-openstack-controller.env").toFile();
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }
    
    @Override
    public void execute() {
        // preconditions:  
        // MTWILSON_HOST, MTWILSON_PORT, and MTWILSON_TLS_CERT_SHA1 must be set ;  note that if using a load balanced mtwilson, the tls cert is for the load balancer
        // the host and port are set by PreconfigureAttestationService, but the tls sha1 fingerprint is set by PostconfigureAttestationService.
        // either way, the sync task forces all attestation service tasks to complete before key broker proxy tasks start, so these settings should be present.
        if( setting("mtwilson.host").isEmpty() || setting("mtwilson.port.https").isEmpty() || setting("mtwilson.tls.cert.sha1").isEmpty() || setting("mtwilson.tag.admin.username").isEmpty() || setting("mtwilson.tag.admin.password").isEmpty() ) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }
        
        // the project name, username, and password are used in PostconfigureOpenstack
        String openstackProjectName = setting("openstack.project.name");
        if( openstackProjectName.isEmpty() ) {
            openstackProjectName = "cit";
            setting("openstack.project.name", openstackProjectName);
        }
        String openstackUsername = setting("openstack.admin.username");
        if( openstackUsername.isEmpty() ) {
            setting("openstack.admin.username", "cit-admin");
        }
        String openstackPassword = setting("openstack.admin.password");
        if( openstackPassword.isEmpty() ) {
            int lengthBytes = 16;
            openstackPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("openstack.admin.password", openstackPassword);
        }
        
        // horizon url
        String endpointUrl = setting("openstack.horizon.url"); // openstack equivalent of mtwilson.endpoint.portal
        if( endpointUrl.isEmpty() ) {
            setting("openstack.horizon.url", "http://" + target.getHost() +"/horizon");    // assuming default horizon port 80
        }
        
        
        //  if trust director will be installed somewhere, and glance settings have not been specified, we generate them here assuming glance is on the same openstack controller
        String glanceTenant = setting("director.glance.tenant");
        if( glanceTenant.isEmpty() ) {
            setting("director.glance.tenant", openstackProjectName);
        }
        String glanceHost = setting("director.glance.host");
        if( glanceHost.isEmpty() ) {
            setting("director.glance.host", target.getHost());
        }
        String glancePort = setting("director.glance.port");
        if( glancePort.isEmpty() ) {
            setting("director.glance.port", "9292"); // openstack glance default port
        }
        String directorUsername = setting("director.glance.username"); 
        if (directorUsername.isEmpty()) {
            setting("director.glance.username", "director");
        }
        String directorPassword = setting("director.glance.password");
        if (directorPassword.isEmpty()) {
            int lengthBytes = 16;
            directorPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("director.glance.password", directorPassword);
        }
        
        // if kms will be installed somewhere with barbican integration, and barbican settings have not been specified, we generate them here assuming barbican is on the same openstack controller
        String barbicanTenant = setting("kms.barbican.project");
        if( barbicanTenant.isEmpty() ) {
            setting("kms.barbican.project", openstackProjectName);
        }
        String barbicanHost = setting("kms.barbican.host");
        if( barbicanHost.isEmpty() ) {
            setting("kms.barbican.host", target.getHost());
        }
        String barbicanPort = setting("kms.barbican.port");
        if( barbicanPort.isEmpty() ) {
            setting("kms.barbican.port", "9311"); // openstack barbican default port
        }
        String barbicanUrl = setting("kms.barbican.url");
        if( barbicanUrl.isEmpty() ) {
            setting("kms.barbican.url", "http://"+setting("kms.barbican.host")+":"+setting("kms.barbican.port"));
        }
        
                
        // the PreconfigureAttestationService task must already be executed 
        data.put("MTWILSON_HOST", setting("mtwilson.host"));
        data.put("MTWILSON_PORT", setting("mtwilson.port.https"));
        data.put("MTWILSON_TAG_ADMIN_USERNAME", setting("mtwilson.tag.admin.username"));
        data.put("MTWILSON_TAG_ADMIN_PASSWORD", setting("mtwilson.tag.admin.username"));
        // the PostconfigureAttestationService task must already be executed 
        data.put("MTWILSON_TLS_CERT_SHA1", setting("mtwilson.tls.cert.sha1"));
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
