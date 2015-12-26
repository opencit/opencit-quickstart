/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.mtwilson.util.task.Task;
import java.util.ArrayList;

/**
 * Adds a capability to call listeners before and after running a task.
 * 
 * @author jbuhacoff
 */
public class TaskRunListenerDecorator extends AbstractTaskDecorator {
    private final ArrayList<TaskRunListener> listeners;

    public TaskRunListenerDecorator(Task delegate) {
        super(delegate);
        this.listeners = new ArrayList<>();
    }

    @Override
    public void run() {
        for (TaskRunListener listener : listeners) {
            listener.beforeRun(delegate);
        }
        delegate.run();
        for (TaskRunListener listener : listeners) {
            listener.afterRun(delegate);
        }
    }

    public ArrayList<TaskRunListener> getListeners() {
        return listeners;
    }
    
}
