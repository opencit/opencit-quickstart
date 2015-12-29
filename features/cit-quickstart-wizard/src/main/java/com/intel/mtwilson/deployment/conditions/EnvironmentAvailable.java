/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.conditions;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.util.task.Condition;
import java.io.File;

/**
 *
 * @author jbuhacoff
 */
public class EnvironmentAvailable implements Condition {
    private String environmentName;

    public EnvironmentAvailable(String environmentName) {
        this.environmentName = environmentName;
    }
    
    
    @Override
    public boolean test() {
        File featuresFile = new File(Folders.repository("cit-features") + File.separator + environmentName + ".json");
        return featuresFile.exists() && featuresFile.isFile() && featuresFile.canRead();
    }
    
}
