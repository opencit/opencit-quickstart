/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

/**
 * This class doesn't do anything, it just shows how existing code may be
 * affected when we replace the original Task base class with AbstractTask.
 * 
 * Procedure to replace:
 * 1. rename original Task class to AbstractTask
 * 2. copy new AbstractTask class to replace original (now renamed) AbstractTask
 * 
 * @author jbuhacoff
 */
public class TaskParentClassComparison {
    
    public static class OriginalTask extends com.intel.dcsg.cpg.performance.Task {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OriginalTask.class);
        
        @Override
        protected void execute() throws Exception {
            log.debug("Original task running");
        }
        
    }
    
    public static class NewTask extends com.intel.mtwilson.util.task.AbstractTask {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewTask.class);
        
        @Override
        public void execute() {
            log.debug("New task running");
        }
        
    }
}
