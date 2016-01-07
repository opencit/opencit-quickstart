/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.util.task.Condition;
import com.intel.mtwilson.util.task.Task;
import com.intel.mtwilson.util.task.TaskManager;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class OrderTaskManager extends TaskManager {

    public OrderTaskManager() {
        super();
        precondition(new NoFaults(this));
        precondition(new Complete(this));
    }
    
    public OrderTaskManager(List<Task> tasks) {
        super(tasks);
        precondition(new NoFaults(this));
        precondition(new Complete(this));
    }
    
    public static class NoFaults implements Condition {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NoFaults.class);

        private TaskManager taskManager;

        public NoFaults(TaskManager taskManager) {
            this.taskManager = taskManager;
        }
        
        @Override
        public boolean test() {
            if( taskManager.getFaults() == null || taskManager.getFaults().isEmpty() ) {
                log.debug("Found no faults");
                return true;
            }
            else {
                log.debug("Found {} faults, setting order status to ERROR", taskManager.getFaults().size());
                return false;
            }
        }
        
    }
    
    public static class Complete implements Condition {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Complete.class);
        private TaskManager taskManager;

        public Complete(TaskManager taskManager) {
            this.taskManager = taskManager;
        }
        
        @Override
        public boolean test() {
            if( taskManager.getCurrent() == taskManager.getMax() ) {
                log.debug("Task manager completed all tasks");
                return true;
            }
            else {
                log.debug("Task manager completed only {} of {} tasks", taskManager.getCurrent(), taskManager.getMax());
                return false;
            }
        }
        
    }
}
