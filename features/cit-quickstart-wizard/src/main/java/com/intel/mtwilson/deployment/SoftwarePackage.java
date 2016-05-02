/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.util.task.Dependencies;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackage implements Dependencies<SoftwarePackage> {
    private final ArrayList<SoftwarePackage> dependencies = new ArrayList<>();
    private final ArrayList<SoftwarePackage> requirements = new ArrayList<>();
    private final String packageName;
    private final Map<String, List<File>> filesMap;

    public SoftwarePackage(String packageName, Map<String, List<File>> filesMap) {
        this.packageName = packageName;
        this.filesMap = filesMap;
    }

    public List<File> getFiles(String key) {
        if(filesMap.containsKey(key))
            return filesMap.get(key);
        return null;
    }

    public String getPackageName() {
        return packageName;
    }
    
    public Map<String, List<File>> getFilesMap() {
        return filesMap;
    }

    @Override
    public Collection<SoftwarePackage> getDependencies() {
        return dependencies;
    }

    public ArrayList<SoftwarePackage> getRequirements() {
        return requirements;
    }
    
//    public boolean isFileAvailable() {
//        return file != null && file.exists() && file.canRead();
//    }
}
