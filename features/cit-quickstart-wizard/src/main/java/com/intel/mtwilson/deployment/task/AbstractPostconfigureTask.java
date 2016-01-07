/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.OrderAware;
import com.intel.mtwilson.deployment.TargetAware;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;

/**
 *
 * @author jbuhacoff
 */
public abstract class AbstractPostconfigureTask extends AbstractRemoteTask implements OrderAware, TargetAware {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractPostconfigureTask.class);

    protected OrderDocument order;
    protected Target target;
    
    @Override
    public void setOrderDocument(OrderDocument order) {
        this.order = order;
    }

    @Override
    public void setTarget(Target target) {
        this.target = target;
    }

    public String getHost() {
        return target.getHost();
    }
    
    protected String setting(String key) {
        String value = order.getSettings().get(key);
        if( value == null ) {
            return "";
        }
        return value;
    }
    
    protected void setting(String key, String value) {
        order.getSettings().put(key, value);
    }
    
}
