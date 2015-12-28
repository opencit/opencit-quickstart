/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.descriptor.SSH;

/**
 * Copy one or more files to a remote host. Difference between this class
 * and FileTransfer is that the list of files to transfer is not known 
 * in advance, and is obtained during execute().
 *
 * @author jbuhacoff
 */
public class DynamicFileTransfer extends FileTransfer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DynamicFileTransfer.class);
    private FileTransferManifestProvider manifestProvider;

    public DynamicFileTransfer(SSH remote, FileTransferManifestProvider manifestProvider) {
        super(remote, (FileTransferDescriptor)null);
        this.manifestProvider = manifestProvider;
    }

    @Override
    public void execute() {
        // precondition:  file transfer manifest available and all source files exist and readable
        manifest = manifestProvider.getFileTransferManifest();
        super.execute();
    }

    /**
     * Returns the actual filenames only AFTER execute(). 
     * Prior to execute() it just returns the literal text "files"
     * @return 
     */
    @Override
    public String getFilenameCsv() {
        if( manifest == null ) { return "files"; }
        return super.getFilenameCsv();
    }

    public boolean isManifestAvailable() {
        return manifest != null;
    }

}
