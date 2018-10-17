/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.conditions;

import com.intel.mtwilson.deployment.Feature;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.SoftwarePackageUtils;
import com.intel.mtwilson.util.task.Condition;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Given a set of selected features, tests that all the required software
 * packages for those features and their feature-dependencies are included.
 * If any required software package is not included, the condition test 
 * returns false and the set of software packages not included can be obtained
 * via getNotIncluded()
 *
 * for example, if input includes a feature with "attestation_host_xm" but does not
 * include any target with software package "director", this test would fail because
 * the trust director is required to generate the manifest for tboot-xm. 
 *
 * @author jbuhacoff
 */
public class FeatureRequiredSoftwarePackagesIncluded implements Condition {
    private Collection<Feature> features;
    private Collection<SoftwarePackage> selected;
    private Set<String> notIncluded = null;

    public FeatureRequiredSoftwarePackagesIncluded(Collection<Feature> features, Collection<SoftwarePackage> selected) {
        this.features = features;
        this.selected = selected;
    }

    @Override
    public boolean test() {
        // make a list of selected names (which we will test to see
        // if it includes all the necessary dependencies)
        HashSet<String> included = new HashSet<>();
        included.addAll(SoftwarePackageUtils.listSoftwarePackageNames(selected));
        
        // now for each selected feature, check if all its software dependencies
        // are included
        notIncluded = new HashSet<>();
        for (Feature feature : features) {
            List<String> requirements = SoftwarePackageUtils.listSoftwarePackageNames(feature.getSoftwarePackages());
            // second, check if any of these dependencies are not included
            // NOTE: we could use Collection.containsAll to just check, but
            // then we wouldn't also be able to report WHICH ones are missing
            for (String requirement : requirements) {
                if (!included.contains(requirement)) {
                    notIncluded.add(requirement);
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
