/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task.faults;

import com.intel.mtwilson.util.validation.faults.Thrown;

/**
 *
 * @author jbuhacoff
 */
public class ExecutionFailed extends Thrown {

    public ExecutionFailed(Throwable cause) {
        super(cause, "Execution failed: %s", cause.getClass().getName());
    }

}
