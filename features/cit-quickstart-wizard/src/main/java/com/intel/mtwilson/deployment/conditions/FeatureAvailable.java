/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.conditions;

import com.intel.mtwilson.deployment.Feature;
import com.intel.mtwilson.util.task.Condition;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Given a set of available feature names and a set of selected
 * feature names, tests that each selected name is available.
 * If any selection is not available, the condition test returns false and
 * the set of selected features not available can be obtained
 * via getNotAvailable().
 * 
 * @author jbuhacoff
 */
public class FeatureAvailable implements Condition {
    private Map<String,Feature> available;
    private Collection<String> selected;
    private Set<String> notAvailable = null;

    public FeatureAvailable(Map<String,Feature> available, Collection<String> selected) {
        this.available = available;
        this.selected = selected;
    }
    
    @Override
    public boolean test() {
        notAvailable = new HashSet<>();
        for(String item : selected) {
            if( !available.containsKey(item) ) {
                notAvailable.add(item);
            }
        }
        return notAvailable.isEmpty();
    }

    /**
     * 
     * @return set of software packages not available (may be empty) or null if the test was not yet performed
     */
    public Set<String> getNotAvailable() {
        return notAvailable;
    }

}
