/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.wizard;

import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author jbuhacoff
 */
public class OrderTransformer {
    private OrderDocument order;

    public OrderTransformer(OrderDocument order) {
        this.order = order;
    }
    
    public void consolidateTargets() {
        HashMap<String,Target> hostMap = new HashMap<>();
        Set<Target> targets = order.getTargets();
        if( targets == null ) {
            order.setTargets(targets);
            return;
        }
        for(Target target : targets) {
            Target existing = hostMap.get(target.getHost());
            if( existing == null ) {
                hostMap.put(target.getHost(), target);
            }
            else {
                // TODO:  we also need to consider username and port, BUT if we do that, then we need some other way to keep track of all software packages to eb installed on same host even if by different targets (host/port/username combinations) so that ports can still be coordinated correctly by the preconfigure tasks
                existing.getPackages().addAll(target.getPackages());
            }
        }
        // now reset the targets to the consolidated set
        order.getTargets().clear();
        order.getTargets().addAll(hostMap.values());
    }
}
