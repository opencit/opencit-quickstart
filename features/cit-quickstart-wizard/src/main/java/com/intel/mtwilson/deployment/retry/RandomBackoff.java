/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.retry;

import org.apache.commons.lang.math.RandomUtils;

/**
 *
 * @author jbuhacoff
 */
public class RandomBackoff implements Backoff {
    private long min, range;

    public RandomBackoff(long min, long max) {
        this.min = min;
        this.range = max-min;
    }

    @Override
    public long getMilliseconds() {
        long random = RandomUtils.nextLong();
        return random % range + min;
    }
    
}
