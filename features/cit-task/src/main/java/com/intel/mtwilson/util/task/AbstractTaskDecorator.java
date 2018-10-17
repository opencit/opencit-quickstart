/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.mtwilson.util.task.Task;
import com.intel.dcsg.cpg.validation.Fault;
import java.util.Collection;

/**
 *
 * @author jbuhacoff
 */
public abstract class AbstractTaskDecorator implements TaskDecorator {
    protected final Task delegate;

    public AbstractTaskDecorator(Task delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public Task getDelegate() {
        return delegate;
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public Collection<Task> getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public Collection<Fault> getFaults() {
        return delegate.getFaults();
    }
    
}
