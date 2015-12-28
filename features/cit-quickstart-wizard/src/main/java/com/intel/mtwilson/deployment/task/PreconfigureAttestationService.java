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

public class PreconfigureAttestationService extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureAttestationService.class);
    private List<FileTransferDescriptor> manifest;
    private File envFile;

    /**
     * Initializes the task with a file transfer manifest; the file(s) mentioned
     * in the manifest will not be available until AFTER execute() completes
     * successfully.
     */    
    public PreconfigureAttestationService() {
        super(); // initializes taskDirectory
        envFile = new File(taskDirectory.getAbsolutePath() + File.separator + "mtwilson.env");
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }

    
    
    @Override
    public void execute() {
        // collect all pre-configuration data, and
        // make the same settings available globally to other packages that may need them
        order.getSettings().put("mtwilson.host", target.getHost());
        
        port(); // stores in order settings  mtwilson.port.http and mtwilson.port.https
        passwords();
        
        String username;
        username = order.getSettings().get("mtwilson.admin.username");
        if (username == null || username.isEmpty()) {
            order.getSettings().put("mtwilson.admin.username", "admin");
        }
        username = order.getSettings().get("mtwilson.tag.admin.username");
        if (username == null || username.isEmpty()) {
            order.getSettings().put("mtwilson.tag.admin.username", "tagadmin");
        }
        username = order.getSettings().get("mtwilson.privacyca.download.username");
        if (username == null || username.isEmpty()) {
            order.getSettings().put("mtwilson.privacyca.download.username", "admin");
        }
        
        // make the data available to the mtwilson.env.st4 template
        data.put("MTWILSON_HOST", target.getHost()); // NOTE: this may be need to be reconsidered for a high-availability or load-balancing configuration, because we need a distinction between "target host" (which is always target) and "endpoint url" (which may be a global setting for load balancer)
        data.put("MTWILSON_PORT", order.getSettings().get("mtwilson.port.https"));
        
        data.put("MTWILSON_ADMIN_USERNAME", order.getSettings().get("mtwilson.admin.username"));
        data.put("MTWILSON_ADMIN_PASSWORD", order.getSettings().get("mtwilson.admin.password"));
        
        data.put("MTWILSON_TAG_ADMIN_USERNAME", order.getSettings().get("mtwilson.tag.admin.username"));
        data.put("MTWILSON_TAG_ADMIN_PASSWORD", order.getSettings().get("mtwilson.tag.admin.password"));

        data.put("MTWILSON_PRIVACYCA_DOWNLOAD_USERNAME", order.getSettings().get("mtwilson.privacyca.download.username"));
        data.put("MTWILSON_PRIVACYCA_DOWNLOAD_PASSWORD", order.getSettings().get("mtwilson.privacyca.download.password"));
        data.put("DATABASE_PASSWORD", order.getSettings().get("mtwilson.database.password"));
        data.put("MTWILSON_TAG_PROVISION_XML_PASSWORD", order.getSettings().get("mtwilson.tag.provision.xml.password"));
        
        // generate the .env file using pre-configuration data
        render("mtwilson.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (!order.getSettings().containsKey("mtwilson.port.http") || !order.getSettings().containsKey("mtwilson.port.https")) {
            if (target.getPackages().size() == 1) {
            // TODO:  if attestatino service is the only software on this
            // host, then we could just set the port to be 443 so it's easier
            // on the URLs...
                order.getSettings().put("mtwilson.port.http", "8080");
                order.getSettings().put("mtwilson.port.https", "8443");
            } else {
                // TODO:  may also need to reset other tomcat ports (shutdown port, etc.)
                order.getSettings().put("mtwilson.port.http", "18080");
                order.getSettings().put("mtwilson.port.https", "18443");
            }
        }
        
    }

    private void passwords() {
        // TODO:  we could read some password descriptors from the
        // software package configuration file (software package reposistory)
        // to know what settings, env var names, character restrictions, etc.
        // apply to each one, then just loop through that set and apply
        // the same logic:
        // if provided in user input settings, use that, otherwise generate it.
        String password;
        password = order.getSettings().get("mtwilson.admin.password");
        if (password == null || password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            order.getSettings().put("mtwilson.admin.password", password);
        }
        password = order.getSettings().get("mtwilson.privacyca.download.password");
        if (password == null || password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            order.getSettings().put("mtwilson.privacyca.download.password", password);
        }
        password = order.getSettings().get("mtwilson.database.password");
        if (password == null || password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            order.getSettings().put("mtwilson.database.password", password);
        }
        password = order.getSettings().get("mtwilson.tag.admin.password");
        if (password == null || password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            order.getSettings().put("mtwilson.tag.admin.password", password);
        }
        password = order.getSettings().get("mtwilson.tag.provision.xml.password");
        if (password == null || password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            order.getSettings().put("mtwilson.tag.provision.xml.password", password);
        }
        
    }

    @Override
    public String getPackageName() {
        return "attestation_service";
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
