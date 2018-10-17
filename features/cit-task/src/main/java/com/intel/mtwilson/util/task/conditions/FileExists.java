/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task.conditions;

import com.intel.mtwilson.util.task.Condition;
import java.io.File;

/**
 *
 * @author jbuhacoff
 */
public class FileExists implements Condition {
    private File file;

    public FileExists(File file) {
        this.file = file;
    }
    
    
    @Override
    public boolean test() {
        return file != null && file.exists();
    }

    public File getFile() {
        return file;
    }
    
}
