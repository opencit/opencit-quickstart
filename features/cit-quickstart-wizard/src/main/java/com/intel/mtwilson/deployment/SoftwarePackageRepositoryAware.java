/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.SoftwarePackageRepository;

/**
 * Implemented by tasks that may need to access the files in a software package.
 * 
 * @author jbuhacoff
 */
public interface SoftwarePackageRepositoryAware {

    void setSoftwarePackageRepository(SoftwarePackageRepository softwarePackageRepository);
    
}
