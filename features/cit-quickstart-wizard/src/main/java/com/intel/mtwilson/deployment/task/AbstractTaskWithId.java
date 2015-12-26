/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.Id;
import com.intel.mtwilson.util.task.AbstractTask;
import java.util.UUID;

/**
 *
 * @author jbuhacoff
 */
public abstract class AbstractTaskWithId extends AbstractTask implements Id {

    private String id;

    public AbstractTaskWithId() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    public AbstractTaskWithId(String id) {
        super();
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }
}
