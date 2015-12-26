/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task.faults;

import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.util.task.Condition;

/**
 *
 * @author jbuhacoff
 */
public class PreconditionFailed extends Fault {
    private Condition precondition;

    public PreconditionFailed(Condition precondition) {
        super("Precondition failed: %s", precondition.toString());
        this.precondition = precondition;
    }
    
    public Condition getPrecondition() {
        return precondition;
    }
    
}
