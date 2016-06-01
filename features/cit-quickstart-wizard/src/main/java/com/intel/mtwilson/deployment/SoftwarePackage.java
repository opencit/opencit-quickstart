/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.util.task.Dependencies;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
        if(filesMap.containsKey(key)) {
            return filesMap.get(key);
        }
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
    
    /**
     * @param name
     * @return true if all the files defined in the file map for that variant are present;  false if any defined file is missing
     */
    private boolean isVariantAvailable(String name) {
        List<File> files = filesMap.get(name);
        if( files == null || files.isEmpty() ) {
            return false;
        }
        for(File file : files) {
            if(file == null || !file.exists() || !file.canRead()){
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns the set of keys that can be used with the map returned
     * by getFilesMap to deploy the software package. For example,
     * Key Broker might only have one variant defined, "default", 
     * so the return value from this method would be [ "default" ] when
     * Key Broker files are available, and [ ]  when Key Broker files are
     * not available (the integrity-only distribution). 
     * Another example, Trust Agent may have two variants defined, "ubuntu"
     * and "redhat", so when both are available the return value would
     * be [ "ubuntu", "redhat " ] but if RedHat files are missing  and 
     * only Ubuntu files are available, the return value would be [ "ubuntu" ]
     * 
     * @return 
     */
    public Set<String> getAvailableVariants() {
        HashSet<String> set = new HashSet<>();
        for(String key : filesMap.keySet()) {
            if( isVariantAvailable(key) ) {
                set.add(key);
            }
        }
        return set;
    }
}
