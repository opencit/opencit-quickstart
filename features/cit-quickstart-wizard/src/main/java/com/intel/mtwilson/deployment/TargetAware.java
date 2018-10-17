/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.descriptor.Target;

/**
 * Implemented by tasks that work on a single target host to
 * copy, pre-configure, install, and/or post-configure osftware 
 * on that host
 * 
 * @author jbuhacoff
 */
public interface TargetAware {

    void setTarget(Target target);
    
}
