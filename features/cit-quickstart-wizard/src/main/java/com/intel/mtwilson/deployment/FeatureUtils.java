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
public class FeatureUtils {
    
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
     * @param features
     * @param output 
     */
    public static void collectAllFeatureNames(Collection<Feature> features, Set<String> output) {
        for(Feature item : features) {
            if( !output.contains(item.getName())) {
                output.add(item.getName());
                collectAllFeatureNames(item.getDependencies(), output);
            }
        }        
    }

    public static List<String> listFeatureNames(Collection<Feature> features) {
        ArrayList<String> list = new ArrayList<>();
        for(Feature item : features) {
            list.add(item.getName());
        }
        return list;
    }

    public static Map<String,Feature> mapFeatures(Collection<Feature> features) {
        HashMap<String,Feature> map = new HashMap<>();
        for(Feature feature : features) {
            if( feature != null ) {
                map.put(feature.getName(), feature);
            }
        }
        return map;
    }
    
}
