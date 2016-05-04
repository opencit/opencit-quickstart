/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentCollection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author jbuhacoff
 */
public class OrderUtils {
    
    /**
     * Returns a copy of the order with host ssh passwords removed.
     * 
     * @param order
     * @return 
     */
    public static OrderDocument sanitize(OrderDocument order) {
        OrderDocument clean = new OrderDocument();
        clean.setId(order.getId());
        clean.getLinks().putAll(order.getLinks());
        clean.getMeta().putAll(order.getMeta());
        clean.setFaultDescriptors(order.getFaultDescriptors());
        clean.setFeatures(order.getFeatures());
        clean.setCreatedOn(order.getCreatedOn());
        clean.setModifiedOn(order.getModifiedOn());
        clean.setProgress(order.getProgress());
        clean.setProgressMax(order.getProgressMax());
        clean.setStatus(order.getStatus());
        clean.setNetworkRole(order.getNetworkRole());
        clean.setSettings(order.getSettings()); // settings may have input or generated passwords for services but user needs to know these
        Set<Target> targets = order.getTargets();
        if( targets != null ) {
            HashSet<Target> cleanTargets = new HashSet<>();
            for(Target target : targets) {
                Target cleanTarget = new Target();
                cleanTarget.setHost(target.getHost());
//                cleanTarget.setNetworkRole(target.getNetworkRole());
                cleanTarget.setPackages(target.getPackages());
                cleanTarget.setPackagesInstalled(target.getPackagesInstalled());
                cleanTarget.setPassword(null); // intentional
                cleanTarget.setPort(target.getPort());
                cleanTarget.setPublicKeyDigest(target.getPublicKeyDigest());
                cleanTarget.setTimeout(null); // intentional
                cleanTarget.setUsername(null); // intentional
                cleanTargets.add(cleanTarget);
            }
            clean.setTargets(cleanTargets);
        }
        clean.setTasks(order.getTasks());
        return clean;
    }
    
    /**
     * Returns a copy of the order collection where each order in the copy
     * has been sanitized.
     * @param collection
     * @return 
     */
    public static OrderDocumentCollection sanitizeCollection(OrderDocumentCollection collection) {
        OrderDocumentCollection clean = new OrderDocumentCollection();
        clean.getLinked().putAll(collection.getLinked());
        clean.getLinks().putAll(collection.getLinks());
        clean.getMeta().putAll(collection.getMeta());
        for(OrderDocument order : collection.getOrders()) {
            clean.getOrders().add(sanitize(order));
        }
        return clean;
    }    
}
