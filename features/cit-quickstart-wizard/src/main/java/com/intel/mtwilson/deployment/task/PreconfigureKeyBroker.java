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
public class PreconfigureKeyBroker extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureKeyBroker.class);
    private List<FileTransferDescriptor> manifest;

    @Override
    public void execute() {
        port();
        data.put("JETTY_PORT", order.getSettings().get("kms.port.http"));
        data.put("JETTY_SECURE_PORT", order.getSettings().get("kms.port.https"));

        // generate the .env file using pre-configuration data
        File envFile = render("kms.env.st4", "kms.env");
        // collect all pre-configuration data...
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, "kms.env"));
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
