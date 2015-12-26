/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import java.util.Objects;

/**
 *
 * @author jbuhacoff
 */
public class FaultDescriptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FaultDescriptor.class);

    private String type;
    private String description;

    public FaultDescriptor() {
    }

    public FaultDescriptor(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isInstance(obj)) {
            return false;
        }
        FaultDescriptor other = (FaultDescriptor) obj;
        String otherType = other.getType();
        String otherDescription = other.getDescription();
        boolean equalType = false;
        boolean equalDescription = false;
        if (type == null && otherType == null || type != null && otherType != null && type.equals(otherType)) {
            equalType = true;
        }
        if (description == null && otherDescription == null || description != null && otherDescription != null && description.equals(otherDescription)) {
            equalDescription = true;
        }
        log.debug("equalType? {}", equalType);
        log.debug("equalDescription? {}", equalDescription);
        return equalType && equalDescription;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.type);
        hash = 83 * hash + Objects.hashCode(this.description);
        return hash;
    }
    
}
