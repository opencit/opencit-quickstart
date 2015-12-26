/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

/**
 *
 * @author jbuhacoff
 */
public interface Condition {
    /**
     * 
     * @return true if the condition is met; false otherwise
     */
    boolean test();
}
