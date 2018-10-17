/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author jbuhacoff
 */
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class Action {
    private String host;
    private Type type;
    private Integer current;
    private Integer max;
    private Status status;
    private String packageName; // only if type is package

    public static enum Type {
        PACKAGE, CONFIGURATION
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    
    
}
