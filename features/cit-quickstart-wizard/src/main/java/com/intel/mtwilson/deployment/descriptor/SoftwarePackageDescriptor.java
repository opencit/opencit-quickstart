/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

import java.util.Collection;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackageDescriptor {
    private String packageName;
    private String fileName;
    private Collection<String> dependencies;

    public String getPackageName() {
        return packageName;
    }

    public String getFileName() {
        return fileName;
    }

    public Collection<String> getDependencies() {
        return dependencies;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setDependencies(Collection<String> dependencies) {
        this.dependencies = dependencies;
    }
    
}
