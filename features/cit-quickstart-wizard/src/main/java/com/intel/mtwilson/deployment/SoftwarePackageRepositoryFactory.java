/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.Folders;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackageRepositoryFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SoftwarePackageRepositoryFactory.class);
    private static final Object instanceLock = new Object();
    private static JsonSoftwarePackageRepository instance;
    
    public static SoftwarePackageRepository getInstance() throws IOException {
        synchronized(instanceLock) {
            if( instance == null ) {
                File file = new File(Folders.repository("software-packages") + File.separator + "software-packages.json");
                instance = new JsonSoftwarePackageRepository(new FileInputStream(file));
            }
        }
        return instance;
    }

}
