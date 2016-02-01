/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.retry;

/**
 * 
 * @author jbuhacoff
 */
public class NearestBackoff implements Backoff {
    private long nearest;
    private Backoff backoff;

    public NearestBackoff(long nearest, Backoff backoff) {
        this.nearest = nearest;
        this.backoff = backoff;
    }

    @Override
    public long getMilliseconds() {
        return ((backoff.getMilliseconds() + nearest-1)/nearest) * nearest;
    }
    
}
