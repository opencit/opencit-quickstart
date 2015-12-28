/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.util.task.Dependencies;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackage implements Dependencies<SoftwarePackage> {
    private final ArrayList<SoftwarePackage> dependencies = new ArrayList<>();
    private final ArrayList<SoftwarePackage> requirements = new ArrayList<>();
    private final String packageName;
    private final File file;

    public SoftwarePackage(String packageName, File file) {
        this.packageName = packageName;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public Collection<SoftwarePackage> getDependencies() {
        return dependencies;
    }

    public ArrayList<SoftwarePackage> getRequirements() {
        return requirements;
    }
    
    public boolean isFileAvailable() {
        return file != null && file.exists() && file.canRead();
    }
}
