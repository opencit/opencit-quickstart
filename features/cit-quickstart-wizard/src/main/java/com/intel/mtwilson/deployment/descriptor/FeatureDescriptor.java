/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

import java.util.ArrayList;

/**
 *
 * @author jbuhacoff
 */
public class FeatureDescriptor {
    private String name;
    private ArrayList<String> softwarePackages;
    private ArrayList<String> dependencies;

    public String getName() {
        return name;
    }

    public ArrayList<String> getSoftwarePackages() {
        return softwarePackages;
    }

    public ArrayList<String> getDependencies() {
        return dependencies;
    }
    
}
