/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.faults;

import com.intel.dcsg.cpg.validation.Fault;

/**
 *
 * @author jbuhacoff
 */
public class FileNotCreated extends Fault {

    public FileNotCreated(String filename) {
        super(filename);
    }
    
}
