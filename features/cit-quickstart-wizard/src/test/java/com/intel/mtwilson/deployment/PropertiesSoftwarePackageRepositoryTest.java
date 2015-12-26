/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.Folders;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class PropertiesSoftwarePackageRepositoryTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PropertiesSoftwarePackageRepositoryTest.class);

    @Test
    public void readSoftwarePropertiesFile() throws IOException {
        HashMap<String,String> packageFileMap = new HashMap<>();
        Properties properties = new Properties();
        try(InputStream in = getClass().getResourceAsStream("/software.properties")) {
            properties.load(in);
            Set<String> packageNames = properties.stringPropertyNames();
            for(String packageName : packageNames) {
                packageFileMap.put(packageName, properties.getProperty(packageName));
            }
        }
        /*
        HashMap<String,String> map = new HashMap<>();
        map.put("key_broker_proxy", "kmsproxy.bin");
        map.put("attestation_service", "attestation_service.bin");
        map.put("key_broker", "kms.bin");
        map.put("openstack_extensions", "openstack_extensions.bin");
        map.put("trust_agent", "trust_agent.bin");
        map.put("trust_director", "trust_director.bin");
        return map;
        */
    }
    
}
