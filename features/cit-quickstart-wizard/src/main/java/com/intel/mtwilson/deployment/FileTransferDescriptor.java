/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import java.io.File;

/**
 *
 * @author jbuhacoff
 */
public class FileTransferDescriptor {
    private File source;
    private String targetPath;
    private Integer permissions;

    public FileTransferDescriptor() {
        this.source = null;
        this.targetPath = null;
        this.permissions = null;
    }

    public FileTransferDescriptor(File source) {
        this.source = source;
        this.targetPath = source.getName();
        this.permissions = null;
    }
    
    public FileTransferDescriptor(File source, String targetPath) {
        this.source = source;
        this.targetPath = targetPath;
        this.permissions = null;
    }
    
    public FileTransferDescriptor(File source, String targetPath, Integer permissions) {
        this.source = source;
        this.targetPath = targetPath;
        this.permissions = permissions;
    }

    public File getSource() {
        return source;
    }

    
    public String getTargetPath() {
        return targetPath;
    }

    public Integer getPermissions() {
        return permissions;
    }
    
    
}
