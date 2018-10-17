/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.conditions;

import com.intel.mtwilson.deployment.Feature;
import com.intel.mtwilson.deployment.FeatureUtils;
import com.intel.mtwilson.util.task.Condition;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Given a set of selected features, tests that all the dependencies for each
 * selected feature are also included in the set of selected features. If any
 * dependency is not selected, the condition test returns false and the set of
 * dependencies not included can be obtained via getNotIncluded()
 *
 * for example, if input includes a feature with "attestation_vm" but does not
 * include any target with "attestation_host", this test would fail because VM
 * attestation depends on host attestation
 *
 * @author jbuhacoff
 */
public class FeatureDependenciesIncluded implements Condition {

    private Collection<Feature> selected;
    private Set<String> notIncluded = null;

    public FeatureDependenciesIncluded(Collection<Feature> selected) {
        this.selected = selected;
    }

    @Override
    public boolean test() {
        // make a list of selected names (which we will test to see
        // if it includes all the necessary dependencies)
        HashSet<String> included = new HashSet<>();
        included.addAll(FeatureUtils.listFeatureNames(selected));

        // now for each selected software package, check if all its dependencies
        // are included
        notIncluded = new HashSet<>();
        for (Feature item : selected) {
            // first, flatten the dependencies for the selected software package
            HashSet<String> dependencies = new HashSet<>();
            FeatureUtils.collectAllFeatureNames(item.getDependencies(), dependencies);
            // second, check if any of these dependencies are not included
            // NOTE: we could use Collection.containsAll to just check, but
            // then we wouldn't also be able to report WHICH ones are missing
            for (String dependency : dependencies) {
                if (!included.contains(dependency)) {
                    notIncluded.add(dependency);
                }
            }
        }
        return notIncluded.isEmpty();
    }

    /**
     *
     * @return set of software packages not included (may be empty) or null if
     * the test was not yet performed
     */
    public Set<String> getNotIncluded() {
        return notIncluded;
    }
}
