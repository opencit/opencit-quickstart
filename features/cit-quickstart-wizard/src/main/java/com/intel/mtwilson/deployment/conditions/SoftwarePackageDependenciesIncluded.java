/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.conditions;

import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.SoftwarePackageUtils;
import com.intel.mtwilson.util.task.Condition;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Given a set of selected software packages, tests that all the dependencies
 * for each selected software package are also included in the set of selected
 * software packages. If any dependency is not selected, the condition test
 * returns false and the set of dependencies not included can be obtained via
 * getNotIncluded()
 *
 * for example, if input includes a target with "key_broker_proxy" but does not
 * include any target with "attestation_service", this test would fail because
 * key_broker_proxy depends on attestation_service to be installed
 *
 * @author jbuhacoff
 */
public class SoftwarePackageDependenciesIncluded implements Condition {
    private Collection<SoftwarePackage> selected;
    private Set<String> notIncluded = null;

    public SoftwarePackageDependenciesIncluded(Collection<SoftwarePackage> selected) {
        this.selected = selected;
    }

    @Override
    public boolean test() {
        // make a list of selected names (which we will test to see
        // if it includes all the necessary dependencies)
        HashSet<String> included = new HashSet<>();
        included.addAll(SoftwarePackageUtils.listSoftwarePackageNames(selected));
        
        // now for each selected software package, check if all its dependencies
        // are included
        notIncluded = new HashSet<>();
        for (SoftwarePackage item : selected) {
            // first, flatten the dependencies for the selected software package
            HashSet<String> dependencies = new HashSet<>();
            SoftwarePackageUtils.collectAllSoftwarePackageNames(item.getDependencies(), dependencies);
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
