/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Test;

/**
 * The configuration model assumes Java properties-like names with identifiers
 * separated by dots, which can be interpreted as opaque identifiers or as
 * path expressions (where mtwilson.database.host is obtained from getDatabase().getHost()).
 * 
 * Each of these settings can be set either by user input (the UI may allow the
 * user to set a value for any property), generated (such as random passwords),
 * obtained from the system prior to installation, or obtained from the system
 * after installation.
 * 
 * 
 * @author jbuhacoff
 */
public class TestConfigurationModel {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestConfigurationModel.class);

    @Test
    public void testSerializeModel() {
        ObjectMapper mapper = new ObjectMapper();
        
    }
    
    public static class DataDictionary {
        public Map<String,DataDefinition> dictionary;
    }
    
    public static class DataDefinition {
//        pu
    }
}
