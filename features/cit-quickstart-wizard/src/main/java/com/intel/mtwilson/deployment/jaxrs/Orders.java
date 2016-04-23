/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentCollection;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentRepository;
import com.intel.mtwilson.deployment.jaxrs.io.OrderFilterCriteria;
import com.intel.mtwilson.deployment.jaxrs.io.OrderLocator;
import com.intel.mtwilson.deployment.threads.OrderDispatchQueue;
import com.intel.mtwilson.jaxrs2.NoLinks;
import com.intel.mtwilson.jaxrs2.Patch;
import com.intel.mtwilson.jaxrs2.mediatype.DataMediaType;
import com.intel.mtwilson.jaxrs2.server.resource.AbstractJsonapiResource;
import com.intel.mtwilson.launcher.ws.ext.V2;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/quickstart/orders")
public class Orders extends AbstractJsonapiResource<OrderDocument, OrderDocumentCollection, OrderFilterCriteria, NoLinks<OrderDocument>, OrderLocator> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Orders.class);
    private OrderDocumentRepository repository;
    
    public Orders() {
        repository = new OrderDocumentRepository();
    }
    
    @Override
    protected OrderDocumentCollection createEmptyCollection() {
        return new OrderDocumentCollection();
    }

    @Override
    protected OrderDocumentRepository getRepository() {
        return repository;
    }
    
        

    /**
     * Successful response:
     * <pre>
     * {"links":{"status":"/v1/orders/9e80cc60-b6c9-417a-8073-0def8a31e53a"}}
     * </pre>
     * 
     * Client can then request GET /v1/orders/9e80cc60-b6c9-417a-8073-0def8a31e53a/tasks
     * to see the status of the task or POST /v1/orders/9e80cc60-b6c9-417a-8073-0def8a31e53a/cancel
     * to cancel the order.
     * 
     * @param locator
     * @param item
     * @param httpServletRequest
     * @param httpServletResponse
     * @return 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public OrderDocument createOne(@BeanParam OrderLocator locator, OrderDocument item, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        OrderDocument created = super.createOne(locator, item, httpServletRequest, httpServletResponse);

        // add links to this order and to the progress monitor
        String orderId = created.getId().toString();
        created.getLinks().put("status", getSelfLink(orderId));
        created.getLinks().put("export", getExportLink(orderId));
        created.getLinks().put("cancel", getCancelLink(orderId));        
//        created.getLinks().put("status", getStatusLink(orderId));
        
        OrderDispatchQueue.getDispatchQueue().add(created);
        
        return sanitize(created);
    }
    
    private OrderDocument sanitize(OrderDocument order) {
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
                cleanTarget.setPublicKeyDigest(null); // intentional
                cleanTarget.setTimeout(null); // intentional
                cleanTarget.setUsername(null); // intentional
                cleanTargets.add(cleanTarget);
            }
            clean.setTargets(cleanTargets);
        }
        clean.setTasks(order.getTasks());
        return clean;
    }
    
    private OrderDocumentCollection sanitizeCollection(OrderDocumentCollection collection) {
        OrderDocumentCollection clean = new OrderDocumentCollection();
        clean.getLinked().putAll(collection.getLinked());
        clean.getLinks().putAll(collection.getLinks());
        clean.getMeta().putAll(collection.getMeta());
        for(OrderDocument order : collection.getOrders()) {
            clean.getOrders().add(sanitize(order));
        }
        return clean;
    }
    
    private String getSelfLink(String orderId) {
        return "/v1/quickstart/orders/"+orderId;
    }
    private String getExportLink(String orderId) {
        return "/v1/quickstart/orders/"+orderId+"/export";
    }
    private String getCancelLink(String orderId) {
        return "/v1/quickstart/orders/"+orderId+"/cancel";
    }
    /*
    private String getStatusLink(String orderId) {
        return "/v1/quickstart/orders/"+orderId+"/tasks";
    }
    */

    @POST
    @Path("{id}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public OrderDocument cancelOrder(@BeanParam OrderLocator locator, @Context HttpServletRequest httpServletRequest, @Context  HttpServletResponse httpServletResponse) {
        OrderDispatchQueue.cancelOrder(locator.id.toString());
        OrderDocument order = repository.retrieve(locator);
        if( order == null ) {
            httpServletResponse.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            return null;
        }
        order.setStatus("CANCELLING");
        return sanitize(order);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public OrderDocument retrieveOne(@BeanParam OrderLocator locator, @Context HttpServletRequest httpServletRequest, @Context  HttpServletResponse httpServletResponse) {
        OrderDocument order = super.retrieveOne(locator, httpServletRequest, httpServletResponse);
        if( order == null ) { return null; }
        return sanitize(order);
    }

    @GET
    @Path("{id}/export")
    @Produces(MediaType.APPLICATION_JSON)
    public OrderDocument exportOrder(@BeanParam OrderLocator locator, @Context HttpServletRequest httpServletRequest, @Context  HttpServletResponse httpServletResponse) {
        OrderDocument order = super.retrieveOne(locator, httpServletRequest, httpServletResponse);
        if( order == null ) { return null; }
        OrderDocument clean = sanitize(order);
        if( "DONE".equals(clean.getStatus()) ) {
            // we don't export task status for completed orders
            clean.setProgress(null);
            clean.setProgressMax(null);
            clean.setTasks(null); 
        }
        return clean;
    }
    
    @GET
    @Produces(DataMediaType.APPLICATION_VND_API_JSON)
    @Override
    public OrderDocumentCollection searchJsonapiCollection(@BeanParam OrderFilterCriteria criteria) {
        OrderDocumentCollection collection = super.searchJsonapiCollection(criteria);
        return sanitizeCollection(collection);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public OrderDocumentCollection searchCollection(OrderFilterCriteria selector) {
        OrderDocumentCollection collection = super.searchCollection(selector);
        return sanitizeCollection(collection);
    }
    

    
    // refuse to let client store or patch orders arbitrarily;  server controls updates to orders
    
    @Override
    public OrderDocumentCollection storeJsonapiCollection(@BeanParam OrderLocator locator, OrderDocumentCollection collection) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public OrderDocument storeOne(@BeanParam OrderLocator locator, OrderDocument item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderDocumentCollection patchJsonapiCollection(@BeanParam OrderLocator locator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderDocument patchOne(@BeanParam OrderLocator locator, Patch<OrderDocument, OrderFilterCriteria, NoLinks<OrderDocument>>[] patchArray) {
        throw new UnsupportedOperationException();
    }

    
}
