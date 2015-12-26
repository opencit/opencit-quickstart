/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.mtwilson.util.task.Task;

/**
 *
 * @author jbuhacoff
 */
public interface TaskDecorator extends Task {
    Task getDelegate();
}
