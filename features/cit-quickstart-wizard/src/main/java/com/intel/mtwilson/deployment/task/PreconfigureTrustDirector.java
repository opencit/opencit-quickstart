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
        // MTWILSON_HOST, MTWILSON_PORT, and MTWILSON_TLS_CERT_SHA1 must be set ;  note that if using a load balanced mtwilson, the tls cert is for the load balancer
        // the host and port are set by PreconfigureAttestationService, but the tls sha1 fingerprint is set by PostconfigureAttestationService.
        // either way, the sync task forces all attestation service tasks to complete before key broker proxy tasks start, so these settings should be present.
        if (!order.getSettings().containsKey("mtwilson.host") || !order.getSettings().containsKey("mtwilson.port.https") || !order.getSettings().containsKey("mtwilson.tls.cert.sha1")) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }

        // precondition:
        // DIRECTOR_MTWILSON_USERNAME and DIRECTOR_MTWILSON_PASSWORD must be set (and corresponding user actually created in mtwilson) 
        // these are done by a separate integration task that must run before this one
        if (!order.getSettings().containsKey("director.mtwilson.username") || !order.getSettings().containsKey("director.mtwilson.password")) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }

        port();
        data.put("JETTY_PORT", order.getSettings().get("director.port.http"));
        data.put("JETTY_SECURE_PORT", order.getSettings().get("director.port.https"));


        // make these global settings available to director.env.st4 template
        data.put("MTWILSON_HOST", order.getSettings().get("mtwilson.host"));
        data.put("MTWILSON_PORT", order.getSettings().get("mtwilson.port.https"));
        data.put("MTWILSON_TLS_CERT_SHA1", order.getSettings().get("mtwilson.tls.cert.sha1"));
        data.put("DIRECTOR_MTWILSON_USERNAME", order.getSettings().get("director.mtwilson.username"));
        data.put("DIRECTOR_MTWILSON_PASSWORD", order.getSettings().get("director.mtwilson.password"));
        data.put("DIRECTOR_ID", "director-" + target.getHost());

        // TODO:   generate DIRECTOR_DB_USERNAME, DIRECTOR_DB_PASSWORD 


        // optional:
        // IF openstack integration is enabled, these settings must be avaiable:
        // openstack.tenant.name, openstack.glance.host, openstack.glance.port, openstack.glance.username, openstack.glance.password
        data.put("OPENSTACK_TENANT_NAME", order.getSettings().get("director.glance.tenant"));
        data.put("OPENSTACK_GLANCE_HOST", order.getSettings().get("director.glance.host"));
        data.put("OPENSTACK_GLANCE_PORT", order.getSettings().get("director.glance.port")); // TODO:  is this http or https?  should make the property name specific , and also if https we will need tls cert sha1 fingerprint
        data.put("DIRECTOR_GLANCE_USERNAME", order.getSettings().get("director.glance.username"));
        data.put("DIRECTOR_GLANCE_PASSWORD", order.getSettings().get("director.glance.password"));

        // optional:
        // IF key broker is enabled, these settings must be available:
        // kms.host, kms.port, kms.username, kms.password, kms.tls.cert.sha1
        data.put("KMS_HOST", order.getSettings().get("kms.host"));
        data.put("KMS_PORT", order.getSettings().get("kms.port.https"));
        data.put("KMS_TLS_CERT_SHA1", order.getSettings().get("kms.tls.cert.sha1")); // or a name from PropertiesTlsPolicyFactory like tls.policy.certificate.sha1
        data.put("DIRECTOR_KMS_LOGIN_BASIC_USERNAME", order.getSettings().get("director.kms.username"));
        data.put("DIRECTOR_KMS_LOGIN_BASIC_PASSWORD", order.getSettings().get("director.kms.password"));

        // generate the .env file using pre-configuration data
        render("director.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (!order.getSettings().containsKey("director.port.http") || !order.getSettings().containsKey("director.port.https")) {
            if (target.getPackages().size() == 1) {
                order.getSettings().put("director.port.http", "80");
                order.getSettings().put("director.port.https", "443");
            } else {
                order.getSettings().put("director.port.http", "19080");
                order.getSettings().put("director.port.https", "19443");
            }
        }
    }

    @Override
    public String getPackageName() {
        return "trust_director";
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
