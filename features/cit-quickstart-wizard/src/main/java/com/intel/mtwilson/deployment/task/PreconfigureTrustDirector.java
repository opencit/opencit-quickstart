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
 * @author jbuhacoff
 */
public class PreconfigureTrustDirector extends AbstractPreconfigureTask implements FileTransferManifestProvider {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureTrustDirector.class);
    private List<FileTransferDescriptor> manifest;
    private File envFile;
    
    /**
     * Initializes the task with a file transfer manifest; the file(s) mentioned
     * in the manifest will not be available until AFTER execute() completes
     * successfully.
     */
    public PreconfigureTrustDirector() {
        super(); // initializes taskDirectory
        envFile = getTaskDirectory().toPath().resolve("director.env").toFile();
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }

    @Override
    public void execute() {
        // preconditions:
        // MTWILSON_HOST, MTWILSON_PORT, and MTWILSON_TLS_CERT_SHA256 must be set ;  note that if using a load balanced mtwilson, the tls cert is for the load balancer
        // the host and port are set by PreconfigureAttestationService, but the tls sha256 fingerprint is set by PostconfigureAttestationService.
        // either way, the sync task forces all attestation service tasks to complete before key broker proxy tasks start, so these settings should be present.
        if (setting("mtwilson.host").isEmpty() || setting("mtwilson.port.https").isEmpty() || setting("mtwilson.tls.cert.sha256").isEmpty()) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }

        // precondition:
        // DIRECTOR_MTWILSON_USERNAME and DIRECTOR_MTWILSON_PASSWORD must be set (and corresponding user actually created in mtwilson) 
        // these are done by a separate integration task that must run before this one
        if (setting("director.mtwilson.username").isEmpty() || setting("director.mtwilson.password").isEmpty()) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }

        setting("director.host", target.getHost());
        
        port();
        
        String endpointUrl = setting("director.endpoint.url");
        if( endpointUrl.isEmpty() ) {
            setting("director.endpoint.url", "https://" + setting("director.host") + ":" + setting("director.port.https"));   
        }
        
        
        data.put("DIRECTOR_PORT_HTTP", setting("director.port.http"));
        data.put("DIRECTOR_PORT_HTTPS", setting("director.port.https"));


        // make these global settings available to director.env.st4 template
        data.put("MTWILSON_HOST", setting("mtwilson.host"));
        data.put("MTWILSON_PORT", setting("mtwilson.port.https"));
        data.put("MTWILSON_TLS_CERT_SHA256", setting("mtwilson.tls.cert.sha256"));
        data.put("DIRECTOR_MTWILSON_USERNAME", setting("director.mtwilson.username"));
        data.put("DIRECTOR_MTWILSON_PASSWORD", setting("director.mtwilson.password"));
        data.put("DIRECTOR_ID", "director-" + target.getHost());

        // TODO:   generate DIRECTOR_DB_USERNAME, DIRECTOR_DB_PASSWORD 


        // optional:
        // IF openstack integration is enabled, these settings must be avaiable:
        // openstack.tenant.name, openstack.glance.host, openstack.glance.port, openstack.glance.username, openstack.glance.password
        data.put("OPENSTACK_TENANT_NAME", setting("director.glance.tenant"));
        data.put("OPENSTACK_GLANCE_URL", setting("director.glance.url"));
        data.put("OPENSTACK_KEYSTONE_URL", setting("director.keystone.url"));
//        data.put("OPENSTACK_GLANCE_HOST", setting("director.glance.host"));
//        data.put("OPENSTACK_GLANCE_PORT", setting("director.glance.port")); // TODO:  is this http or https?  should make the property name specific , and also if https we will need tls cert sha256 fingerprint
        data.put("DIRECTOR_GLANCE_USERNAME", setting("director.glance.username"));
        data.put("DIRECTOR_GLANCE_PASSWORD", setting("director.glance.password"));

        // optional:
        // IF key broker is enabled, these settings must be available:
        // kms.host, kms.port, kms.username, kms.password, kms.tls.cert.sha256
        data.put("KMS_HOST", setting("kms.host"));
        data.put("KMS_PORT", setting("kms.port.https"));
        data.put("KMS_TLS_CERT_SHA256", setting("kms.tls.cert.sha256")); // or a name from PropertiesTlsPolicyFactory like tls.policy.certificate.sha256
        data.put("DIRECTOR_KMS_LOGIN_BASIC_USERNAME", setting("director.kms.username"));
        data.put("DIRECTOR_KMS_LOGIN_BASIC_PASSWORD", setting("director.kms.password"));

        // the admin username and password are generated here and stored in settings
        // but not added to .env file because key broker .env does not support
        // creating the admin user that way. so we create it later in the postconfigure task.
        String username = setting("director.admin.username");
        if ( username.isEmpty()) {
            setting("director.admin.username", "admin");
        }
        String password = setting("director.admin.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("director.admin.password", password);
        }
        String dbUsername = setting("director.database.username");
        if( dbUsername.isEmpty() ) {
            dbUsername = "director";
            setting("director.database.username", dbUsername);
        }
        String dbPassword = setting("director.database.password");
        if( dbPassword.isEmpty() ) {
            int lengthBytes = 16;
            dbPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("director.database.password", dbPassword);
        }
        data.put("DIRECTOR_DATABASE_USERNAME", dbUsername);
        data.put("DIRECTOR_DATABASE_PASSWORD", dbPassword);
        // generate the .env file using pre-configuration data
        render("director.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (setting("director.port.http").isEmpty() || setting("director.port.https").isEmpty()) {
            if (target.getPackages().size() == 1) {
                setting("director.port.http", "81");
                setting("director.port.https", "444");
            } else {
                setting("director.port.http", "19081");
                setting("director.port.https", "19444");
            }
        }
    }

    @Override
    public String getPackageName() {
        return "director";
    }

    /**
     * Must be called AFTER execute() to get list of files that should be
     * transferred to the remote host
     *
     * @return
     */
    @Override
    public List<FileTransferDescriptor> getFileTransferManifest() {
        return manifest;
    }
}
