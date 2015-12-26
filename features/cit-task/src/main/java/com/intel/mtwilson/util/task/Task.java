/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.dcsg.cpg.validation.Faults;

/**
 * The interface includes isDone() which should only return true if the
 * task completed successfully.  Any problems should be 
 * reported via getFaults() which is included in the Faults interface. 
 * 
 * The getDependencies() method returns a collection of tasks that must 
 * be done before this task is started
 * 
 * @author jbuhacoff
 */
public interface Task extends Runnable, Faults, Dependencies<Task> {
    
    /**
     * 
     * @return true if the task was completed; false if it was not started yet, or canceled, or interrupted due to an error
     */
    boolean isDone();
    
}
