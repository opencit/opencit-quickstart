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
public class PostconditionFailed extends Fault {
    private Condition postcondition;

    public PostconditionFailed(Condition postcondition) {
        super("Postcondition failed: %s", postcondition.toString());
        this.postcondition = postcondition;
    }

    public Condition getPostcondition() {
        return postcondition;
    }
    
}
