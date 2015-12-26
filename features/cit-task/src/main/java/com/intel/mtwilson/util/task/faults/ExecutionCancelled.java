/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task.faults;

import com.intel.dcsg.cpg.validation.Fault;

/**
 *
 * @author jbuhacoff
 */
public class ExecutionCancelled extends Fault {

    public ExecutionCancelled() {
        super("Cancelled");
    }

}
