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
public class Null extends Fault {
    private String parameterName;
    
    public Null(String parameterName) {
        super(parameterName);
        this.parameterName = parameterName;
    }
    
    public String getParameterName() {
        return parameterName;
    }    
}
