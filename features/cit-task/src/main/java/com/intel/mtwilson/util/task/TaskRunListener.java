/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.mtwilson.util.task.Task;

/**
 * This listener interface assumes that a single listener instance may be
 * applied to many tasks.
 * 
 * @author jbuhacoff
 */
public interface TaskRunListener {
    void beforeRun(Task task);
    void afterRun(Task task);
}
