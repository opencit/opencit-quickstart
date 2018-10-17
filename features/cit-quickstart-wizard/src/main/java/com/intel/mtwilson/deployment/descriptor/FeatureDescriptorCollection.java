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
public class FeatureDescriptorCollection {
    private Collection<FeatureDescriptor> features;

    public Collection<FeatureDescriptor> getFeatures() {
        return features;
    }

    public void setFeatures(Collection<FeatureDescriptor> features) {
        this.features = features;
    }
    
}
