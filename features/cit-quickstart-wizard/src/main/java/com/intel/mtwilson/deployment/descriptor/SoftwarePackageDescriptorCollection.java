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
public class SoftwarePackageDescriptorCollection {
    private Collection<SoftwarePackageDescriptor> softwarePackages;

    public Collection<SoftwarePackageDescriptor> getSoftwarePackages() {
        return softwarePackages;
    }

    public void setSoftwarePackages(Collection<SoftwarePackageDescriptor> softwarePackages) {
        this.softwarePackages = softwarePackages;
    }
    
}
