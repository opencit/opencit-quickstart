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
public class PreconfigureKeyBroker extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureKeyBroker.class);
    private List<FileTransferDescriptor> manifest;
    private File envFile;
    
    /**
     * Initializes the task with a file transfer manifest; the file(s) mentioned
     * in the manifest will not be available until AFTER execute() completes
     * successfully.
     */
    public PreconfigureKeyBroker() {
        super(); // initializes taskDirectory
        envFile = getTaskDirectory().toPath().resolve("kms.env").toFile();
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
    }

    
    
    @Override
    public void execute() {
        setting("kms.host", target.getHost());
        
        port();
        data.put("JETTY_PORT", setting("kms.port.http"));
        data.put("JETTY_SECURE_PORT", setting("kms.port.https"));
        data.put("KEY_MANAGER_PROVIDER", setting("kms.key.provider"));
        data.put("KMIP_ENDPOINT", setting("kms.kmip.url"));
        data.put("BARBICAN_PROJECT_ID", setting("kms.barbican.project"));
        data.put("BARBICAN_ENDPOINT_URL", setting("kms.barbican.url"));
        data.put("BARBICAN_KEYSTONE_PUBLIC_ENDPOINT", setting("kms.keystone.url"));
        data.put("BARBICAN_TENANTNAME", setting("kms.barbican.tenant"));
        data.put("BARBICAN_USERNAME", setting("kms.barbican.username"));
        data.put("BARBICAN_PASSWORD", setting("kms.barbican.password"));

        String endpointUrl = setting("kms.endpoint.url");
        if( endpointUrl.isEmpty() ) {
            setting("kms.endpoint.url", "https://" + setting("kms.host") + ":" + setting("kms.port.https"));   
        }
        String endpointKeytransfer = setting("kms.endpoint.keytransfer");
        if( endpointKeytransfer.isEmpty() ) {
            setting("kms.endpoint.keytransfer", "http://" + setting("kms.host") + ":" + setting("kms.port.http"));   
        }        
        
        // the admin username and password are generated here and stored in settings
        // but not added to .env file because key broker .env does not support
        // creating the admin user that way. so we create it later in the postconfigure task.
        String username = setting("kms.admin.username");
        if ( username.isEmpty()) {
            setting("kms.admin.username", "admin");
        }
        String password = setting("kms.admin.password");
        if ( password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("kms.admin.password", password);
        }
        
        // generate the .env file using pre-configuration data
        render("kms.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (setting("kms.port.http").isEmpty() || setting("kms.port.https").isEmpty()) {
            if (target.getPackages().size() == 1) {
                setting("kms.port.http", "80");
                setting("kms.port.https", "443");
            } else {
                setting("kms.port.http", "20080");
                setting("kms.port.https", "20443");
            }
        }
        
    }
    

    @Override
    public String getPackageName() {
        return "key_broker";
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
