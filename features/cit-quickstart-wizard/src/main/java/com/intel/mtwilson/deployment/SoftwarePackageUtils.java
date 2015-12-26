/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackageUtils {
    
    /**
     * Given some software packages and an output set, adds each of the 
     * software packages and its dependencies to the output so the output
     * represents one set of all the software packages and any dependencies.
     * 
     * Circular dependencies are handled by skipping processing of any 
     * software package that already appears in the output. 
     * 
     * Usage example:
     * <pre>
     * HashSet<String> output = new HashSet<>();
     * collectAllSoftwarePackageNames(softwarePackages, output);
     * // now output has all the software package names including dependencies
     * </pre>
     * 
     * @param softwarePackages
     * @param output 
     */
    public static void collectAllSoftwarePackageNames(Collection<SoftwarePackage> softwarePackages, Set<String> output) {
        for(SoftwarePackage item : softwarePackages) {
            if( !output.contains(item.getPackageName())) {
                output.add(item.getPackageName());
                collectAllSoftwarePackageNames(item.getDependencies(), output);
            }
        }        
    }

    public static List<String> listSoftwarePackageNames(Collection<SoftwarePackage> softwarePackages) {
        ArrayList<String> list = new ArrayList<>();
        for(SoftwarePackage item : softwarePackages) {
            list.add(item.getPackageName());
        }
        return list;
    }
    
    public static Map<String,SoftwarePackage> mapSoftwarePackages(Collection<SoftwarePackage> softwarePackages) {
        HashMap<String,SoftwarePackage> map = new HashMap<>();
        for(SoftwarePackage softwarePackage : softwarePackages) {
            if( softwarePackage != null ) {
                map.put(softwarePackage.getPackageName(), softwarePackage);
            }
        }
        return map;
    }
}
