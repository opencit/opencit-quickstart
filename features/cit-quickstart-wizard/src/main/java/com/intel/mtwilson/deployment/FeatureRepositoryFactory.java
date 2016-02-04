/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.Folders;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jbuhacoff
 */
public class FeatureRepositoryFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureRepositoryFactory.class);
    private static ConcurrentHashMap<String,JsonFeatureRepository> instanceMap = new ConcurrentHashMap<>();
    
    /**
     * 
     * @param edition must be the name (without .json extension) of a file in the "cit-features" directory; currently one of "PRIVATE", "PROVIDER", or "SUBSCRIBER"
     * @return
     * @throws IOException 
     */
    public static FeatureRepository getInstance(String edition) throws IOException {
        SoftwarePackageRepository softwarePackageRepository = SoftwarePackageRepositoryFactory.getInstance();
        JsonFeatureRepository instance = instanceMap.get(edition);
        if( instance == null ) {
            File file = new File(Folders.repository("cit-features") + File.separator + edition + ".json");
            instance = new JsonFeatureRepository(new FileInputStream(file), softwarePackageRepository);
            instanceMap.put(edition, instance);
        }
        return instance;
    }

    
}
