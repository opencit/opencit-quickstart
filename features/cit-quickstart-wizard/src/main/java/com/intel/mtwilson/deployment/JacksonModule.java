/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.util.task.Condition;

/**
 *
 * @author jbuhacoff
 */
public class JacksonModule extends Module {

    @Override
    public String getModuleName() {
        return "QuickstartModule";
    }

    @Override
    public Version version() {
        return new Version(1,0,0,"com.intel.mtwilson.deployment","mtwilson-deployment-json",null);
    }

    @Override
    public void setupModule(SetupContext sc) {
        sc.setMixInAnnotations(Fault.class, FaultTypeMixIn.class);
        sc.setMixInAnnotations(Condition.class, ConditionTypeMixIn.class);
    }
    
}
