/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.Id;
import com.intel.mtwilson.util.task.AbstractTask;
import java.io.File;
import java.util.UUID;

/**
 *
 * @author jbuhacoff
 */
public abstract class AbstractTaskWithId extends AbstractTask implements Id {
    /**
     * A unique identifier for this task
     */
    private String id;
    
    /**
     * A directory where the task can store temporary files for the order.
     * This is initialized in the constructor. Note that the directory itself
     * is NOT created by this abstract parent class; if any subclass needs to
     * store data there it needs to create that directory if it doesn't exist.
     */
    private File taskDirectory;

    public AbstractTaskWithId() {
        super();
        this.id = UUID.randomUUID().toString();
        this.taskDirectory = new File(Folders.repository("tasks") + File.separator + this.id);
    }

    public AbstractTaskWithId(String id) {
        super();
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public File getTaskDirectory() {
        return taskDirectory;
    }
    
}
