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
        envFile = getTaskDirectory().toPath().resolve("mtwilson.env").toFile();
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }

    
    
    @Override
    public void execute() {
        // collect all pre-configuration data, and
        // make the same settings available globally to other packages that may need them
        setting("mtwilson.host", target.getHost());
        
        port(); // stores in order settings  mtwilson.port.http and mtwilson.port.https
        
        String endpointPortal = setting("mtwilson.endpoint.portal");
        if( endpointPortal.isEmpty() ) {
            setting("mtwilson.endpoint.portal", "https://" + setting("mtwilson.host") + ":" + setting("mtwilson.port.https") + "/mtwilson-portal");
        }
        String endpointUrl = setting("mtwilson.endpoint.url");
        if( endpointUrl.isEmpty() ) {
            setting("mtwilson.endpoint.url", "https://" + setting("mtwilson.host") + ":" + setting("mtwilson.port.https") + "/mtwilson");
        }
        
        passwords();
        
        String username;
        username = setting("mtwilson.admin.username");
        if ( username.isEmpty()) {
            setting("mtwilson.admin.username", "admin");
        }
        username = setting("mtwilson.tag.admin.username");
        if ( username.isEmpty()) {
            setting("mtwilson.tag.admin.username", "tagadmin");
        }
        username = setting("mtwilson.privacyca.download.username");
        if ( username.isEmpty()) {
            setting("mtwilson.privacyca.download.username", "pca-admin");
        }
        username = setting("mtwilson.quickstart.username");
        if ( username.isEmpty()) {
            setting("mtwilson.quickstart.username", "quickstart");
        }
        
        // make the data available to the mtwilson.env.st4 template
        data.put("MTWILSON_HOST", target.getHost()); // NOTE: this may be need to be reconsidered for a high-availability or load-balancing configuration, because we need a distinction between "target host" (which is always target) and "endpoint url" (which may be a global setting for load balancer)
        data.put("MTWILSON_PORT", setting("mtwilson.port.https"));
        
        data.put("MTWILSON_ADMIN_USERNAME", setting("mtwilson.admin.username"));
        data.put("MTWILSON_ADMIN_PASSWORD", setting("mtwilson.admin.password"));
        
        data.put("MTWILSON_TAG_ADMIN_USERNAME", setting("mtwilson.tag.admin.username"));
        data.put("MTWILSON_TAG_ADMIN_PASSWORD", setting("mtwilson.tag.admin.password"));

        data.put("MTWILSON_PRIVACYCA_DOWNLOAD_USERNAME", setting("mtwilson.privacyca.download.username"));
        data.put("MTWILSON_PRIVACYCA_DOWNLOAD_PASSWORD", setting("mtwilson.privacyca.download.password"));
        data.put("DATABASE_PASSWORD", setting("mtwilson.database.password"));
        data.put("MTWILSON_TAG_PROVISION_XML_PASSWORD", setting("mtwilson.tag.provision.xml.password"));
        
        // generate the .env file using pre-configuration data
        render("mtwilson.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (setting("mtwilson.port.http").isEmpty() || setting("mtwilson.port.https").isEmpty()) {
            if (target.getPackages().size() == 1) {
            // TODO:  if attestatino service is the only software on this
            // host, then we could just set the port to be 443 so it's easier
            // on the URLs...
                setting("mtwilson.port.http", "8080");
                setting("mtwilson.port.https", "8443");
            } else {
                // TODO:  may also need to reset other tomcat ports (shutdown port, etc.)
                // NOTE: mtwilson port is not configurable right now so we assume it's running on tomcat wtih default ports
                setting("mtwilson.port.http", "8080"); // TODO: change to 18080 when mtwilson is configurable
                setting("mtwilson.port.https", "8443"); // TODO: change to 18443 when mtwilson is configurable
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
        password = setting("mtwilson.admin.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("mtwilson.admin.password", password);
        }
        password = setting("mtwilson.privacyca.download.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("mtwilson.privacyca.download.password", password);
        }
        password = setting("mtwilson.quickstart.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("mtwilson.quickstart.password", password);
        }        
        password = setting("mtwilson.database.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("mtwilson.database.password", password);
        }
        password = setting("mtwilson.tag.admin.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("mtwilson.tag.admin.password", password);
        }
        password = setting("mtwilson.tag.provision.xml.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("mtwilson.tag.provision.xml.password", password);
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
