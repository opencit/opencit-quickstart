/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author jbuhacoff
 */
public class FeatureDescriptor {
    private String name;
    private ArrayList<String> softwarePackages;
    private ArrayList<String> dependencies;
    private ArrayList<String> requiredSettings;

    public String getName() {
        return name;
    }

    public Collection<String> getSoftwarePackages() {
        return softwarePackages;
    }

    public Collection<String> getDependencies() {
        return dependencies;
    }

    public Collection<String> getRequiredSettings() {
        return requiredSettings;
    }
    
    
}
