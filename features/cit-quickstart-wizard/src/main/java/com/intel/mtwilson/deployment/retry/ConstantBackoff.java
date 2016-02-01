/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.retry;

/**
 *
 * @author jbuhacoff
 */
public class ConstantBackoff implements Backoff {
    private long milliseconds;

    public ConstantBackoff(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    @Override
    public long getMilliseconds() {
        return milliseconds;
    }
    
}
