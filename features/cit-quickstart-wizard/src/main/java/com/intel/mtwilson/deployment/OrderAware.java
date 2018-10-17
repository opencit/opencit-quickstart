/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;

/**
 * Implemented by classes that need some or all of the entire order context for what
 * they are need to do.
 * 
 * @author jbuhacoff
 */
public interface OrderAware {
    void setOrderDocument(OrderDocument order);
    
}
