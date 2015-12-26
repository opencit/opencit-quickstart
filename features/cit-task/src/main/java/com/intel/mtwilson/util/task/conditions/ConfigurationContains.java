/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task.conditions;

import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.mtwilson.util.task.Condition;

/**
 *
 * @author jbuhacoff
 */
public class ConfigurationContains implements Condition {
    private Configuration configuration;
    private String key;

    public ConfigurationContains(Configuration configuration, String key) {
        this.configuration = configuration;
        this.key = key;
    }
    
    @Override
    public boolean test() {
        return configuration != null && key != null && configuration.get(key) != null;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getKey() {
        return key;
    }
}
