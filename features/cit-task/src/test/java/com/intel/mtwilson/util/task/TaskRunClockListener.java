/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

/**
 *
 * @author jbuhacoff
 */
public class TaskRunClockListener implements TaskRunListener {
    private long started, stopped;

    public TaskRunClockListener() {
        this.started = 0;
        this.stopped = 0;
    }


    /**
     * 
     * @return time the run started, in milliseconds
     */
    public long getTimeStarted() {
        return started;
    }

    /**
     * 
     * @return time the run stopped, in milliseconds; this can be successful completion or when it stopped due to an exception
     */
    public long getTimeStopped() {
        return stopped;
    }

  

    @Override
    public void beforeRun(Task task) {
        started = System.currentTimeMillis();
        stopped = 0;
    }

    @Override
    public void afterRun(Task task) {
        stopped = System.currentTimeMillis();
   }
    
    
}
