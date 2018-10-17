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
public class ClockingTask extends AbstractTaskDecorator {
    private long started, stopped;

    public ClockingTask(Task delegate) {
        super(delegate);
        started = 0;
        stopped = 0;
    }

    
    @Override
    public void run() {
        started = System.currentTimeMillis();
        stopped = 0;
        delegate.run();
        stopped = System.currentTimeMillis();
    }

    public long getTimeStarted() {
        return started;
    }

    public long getTimeStopped() {
        return stopped;
    }

    
}
