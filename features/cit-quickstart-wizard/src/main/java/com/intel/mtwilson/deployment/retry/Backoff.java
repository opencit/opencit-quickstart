/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.retry;

/**
 *
 * @author jbuhacoff
 */
public interface Backoff {
    long getMilliseconds();
}
