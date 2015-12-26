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
public class ConnectionTimeout extends Fault {
    private String host;
    private int timeout;
    
    public ConnectionTimeout(String host, int timeout) {
        super("Connection to %s timeout after %d seconds", host, timeout/1000);
        this.host = host;
        this.timeout = timeout;
    }

    public String getHost() {
        return host;
    }

    public int getTimeout() {
        return timeout;
    }
    
   
}
