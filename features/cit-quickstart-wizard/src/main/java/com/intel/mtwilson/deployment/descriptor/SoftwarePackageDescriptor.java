/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Dependencies refer to any software package that, if included in the 
 * deployment, must be installed BEFORE this one.
 * 
 * Requirements refer to any software package that MUST be included in the
 * deployment (whether they are installed before or after this one). 
 * 
 * @author jbuhacoff
 */
public class SoftwarePackageDescriptor {
    private String packageName;
    private Map<String, List<String>> fileName;
    private Collection<String> dependencies;
    private Collection<String> requirements;

    public String getPackageName() {
        return packageName;
    }

    public Map<String, List<String>> getFileName() {
        return fileName;
    }

    public Collection<String> getDependencies() {
        return dependencies;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setFileName(Map<String, List<String>> filesMap) {
        this.fileName = filesMap;
    }

    public void setDependencies(Collection<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Collection<String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Collection<String> requirements) {
        this.requirements = requirements;
    }
    
    
}
