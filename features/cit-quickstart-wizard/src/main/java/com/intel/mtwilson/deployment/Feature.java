/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.util.task.Dependencies;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author jbuhacoff
 */
public class Feature implements Dependencies<Feature> {
    private final String name;
    private final ArrayList<SoftwarePackage> softwarePackages = new ArrayList<>();
    private final ArrayList<Feature> dependencies = new ArrayList<>();

    public Feature(String name) {
        this.name = name;
    }

    @Override
    public Collection<Feature> getDependencies() {
        return dependencies;
    }

    public ArrayList<SoftwarePackage> getSoftwarePackages() {
        return softwarePackages;
    }

    public String getName() {
        return name;
    }
    
}
