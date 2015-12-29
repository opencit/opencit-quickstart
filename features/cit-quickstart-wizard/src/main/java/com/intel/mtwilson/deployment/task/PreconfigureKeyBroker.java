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
        port();
        data.put("JETTY_PORT", order.getSettings().get("kms.port.http"));
        data.put("JETTY_SECURE_PORT", order.getSettings().get("kms.port.https"));

        // the admin username and password are generated here and stored in settings
        // but not added to .env file because key broker .env does not support
        // creating the admin user that way. so we create it later in the postconfigure task.
        String username = order.getSettings().get("kms.admin.username");
        if (username == null || username.isEmpty()) {
            order.getSettings().put("kms.admin.username", "admin");
        }
        String password = order.getSettings().get("kms.admin.password");
        if (password == null || password.isEmpty()) {
            int lengthBytes = 16;
            password = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            order.getSettings().put("kms.admin.password", password);
        }
        
        // generate the .env file using pre-configuration data
        render("kms.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (!order.getSettings().containsKey("kms.port.http") || !order.getSettings().containsKey("kms.port.https")) {
            if (target.getPackages().size() == 1) {
                order.getSettings().put("kms.port.http", "80");
                order.getSettings().put("kms.port.https", "443");
            } else {
                order.getSettings().put("kms.port.http", "20080");
                order.getSettings().put("kms.port.https", "20443");
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
