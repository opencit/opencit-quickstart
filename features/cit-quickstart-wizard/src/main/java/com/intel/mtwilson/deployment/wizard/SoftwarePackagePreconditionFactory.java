/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.wizard;

import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.util.task.Condition;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackagePreconditionFactory {

    public List<Condition> createPreconditions(SoftwarePackage softwarePackage, Target target) {
        ArrayList<Condition> conditions = new ArrayList<>();
        return conditions;
    }

    public static interface SoftwarePackagePreconditionGenerator {

        public List<Condition> generate();
    }

    public static class AttestationServicePreconditionGenerator {

        public List<Condition> getConditions() {
            ArrayList<Condition> list = new ArrayList<>();
            list.add(new OperatingSystemMatches(new OperatingSystemInfo("Ubuntu", "*"), new OperatingSystemInfo("Ubuntu", "12.04")));
            return list;
        }
    }

    public static class OperatingSystemMatches implements Condition {
        private OperatingSystemInfo allow, test;

        public OperatingSystemMatches(OperatingSystemInfo allow, OperatingSystemInfo test) {
            this.allow = allow;
            this.test = test;
        }
        
        @Override
        public boolean test() {
            boolean distributor = false;
            boolean version = false;
            if( allow.getDistributor() == null || allow.getDistributor().isEmpty() || allow.getDistributor().equals("*")) {
                distributor = true;
            }
            else if( allow.getDistributor().equals(test.getDistributor()) ) {
                distributor = true;
            }
            if( allow.getVersion() == null || allow.getVersion().isEmpty() ) {
                version = true;
            }
            else if( allow.getVersion().equals(test.getVersion()) ) {
                version = true;
            }
            return distributor && version;
        }
    }
}
