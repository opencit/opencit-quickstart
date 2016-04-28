/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.conditions;

import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.wizard.DeploymentTaskFactory;
import com.intel.mtwilson.util.task.Condition;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Given a set of available software package names and a set of selected
 * software package names, tests that each selected name is available. If any
 * selection is not available, the condition test returns false and the set of
 * selected software packages not available can be obtained via
 * getNotAvailable().
 *
 * for example, if input includes a software package "foo" for which we don't
 * have an installer, this test would fail.
 *
 * @author jbuhacoff
 */
public class SoftwarePackageAvailable implements Condition {
    private Map<String,SoftwarePackage> available;
    private Collection<String> selected;
    private Set<String> notAvailable = null;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SoftwarePackageAvailable.class);

    public SoftwarePackageAvailable(Map<String,SoftwarePackage> available, Collection<String> selected) {
        this.available = available;
        this.selected = selected;
    }
    

    @Override
    public boolean test() {
        notAvailable = new HashSet<>();
        for (String name : selected) {
            SoftwarePackage softwarePackage = available.get(name);
            if( softwarePackage == null ) {
                notAvailable.add(name);
                continue;
            }
            for (List<File> files : softwarePackage.getFilesMap().values()){
                for(File file : files){
                    if(file == null || !file.exists() || !file.canRead()){
                        notAvailable.add(name);
                        log.debug("File not available {} in software package {} ",file==null?null:file.getPath(), name);
                        break;
                    }
                }
            }
        }
        return notAvailable.isEmpty();
    }

    /**
     *
     * @return set of software packages not available (may be empty) or null if
     * the test was not yet performed
     */
    public Set<String> getNotAvailable() {
        return notAvailable;
    }
}
