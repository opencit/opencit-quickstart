/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.util.task.AbstractTask;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jbuhacoff
 */
public class SynchronizeSoftwarePackageTargets extends AbstractTaskWithId {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SynchronizeSoftwarePackageTargets.class);

    private SoftwarePackage softwarePackage;
    private Collection<Target> targets;

    public SynchronizeSoftwarePackageTargets(SoftwarePackage softwarePackage, Collection<Target> targets) {
        super();
        this.softwarePackage = softwarePackage;
        this.targets = targets;
    }

    @Override
    public void execute() {
        log.debug("Synchronizing {}", softwarePackage.getPackageName());
    }

    public String getPackageName() {
        return softwarePackage.getPackageName();
    }

    public String getHostCsv() {
        ArrayList<String> hosts = new ArrayList<>();
        if (targets != null) {
            for (Target target : targets) {
                hosts.add(target.getHost());
            }
        }
        return StringUtils.join(hosts, ", ");
    }
}
