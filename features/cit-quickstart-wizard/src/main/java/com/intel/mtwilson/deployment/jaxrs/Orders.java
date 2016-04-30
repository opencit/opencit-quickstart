/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.intel.mtwilson.deployment.OrderUtils;
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
import com.intel.mtwilson.shiro.Username;
import com.intel.mtwilson.shiro.UsernameWithPermissions;
import com.intel.mtwilson.shiro.authc.token.Token;
import com.intel.mtwilson.shiro.authc.token.TokenAuthenticationToken;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

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
        
        // protect the order by associating EITHER an anonymous user's session token OR an authenticated user's username; later to retrieve the order a client would need to be logged in with that session token (user in the same UI session) or username (same API client or at least shared credentials) 
        String owner = getSubjectIdentity();
        created.getMeta().put("owner", owner);
        log.debug("Created order with owner: {}", owner);
        
        OrderDispatchQueue.getDispatchQueue().add(created);
        
        return OrderUtils.sanitize(created);
    }
    
    private static <T> T first(Collection<T> collection) {
        if( collection == null || collection.isEmpty() ) { return null; }
        Iterator<T> it = collection.iterator();
        if( it.hasNext() ) {
            return it.next();
        }
        return null;
    }
    
    private String getSubjectIdentity() {
        Subject subject = SecurityUtils.getSubject();
        if (!subject.isAuthenticated()) {
            throw new WebApplicationException(Response.noContent().status(Response.Status.UNAUTHORIZED).build());
        }
        PrincipalCollection principals = subject.getPrincipals();
        
        String identity = null;
        Collection<UsernameWithPermissions> usernameWithPermissionsCollection = principals.byType(UsernameWithPermissions.class);
        if( usernameWithPermissionsCollection != null && !usernameWithPermissionsCollection.isEmpty() ) {
            UsernameWithPermissions usernameWithPermissions = first(usernameWithPermissionsCollection);
            if( usernameWithPermissions != null ) {
                identity = usernameWithPermissions.getUsername();
            }
        }
        /*
        if( identity == null ) {
            Collection<Username> usernameCollection = principals.byType(Username.class);
            if( usernameCollection != null && !usernameCollection.isEmpty() ) {
                Username username = first(usernameCollection);
                if( username != null ) {
                    identity = username.getUsername();
                }
            }            
        }
        */
        if( identity != null && identity.equals("anonymous") ) {
            Collection<Token> tokenCollection = principals.byType(Token.class);
            if( tokenCollection != null && !tokenCollection.isEmpty() ) {
                Token token = first(tokenCollection);
                if( token != null ) {
                    identity = String.format("token:%s", token.getValue());
                }
            }            
        }
        return identity;
        /*
        UsernameWithPermissions usernameWithPermissions = LoginTokenUtils.getFirstElementFromCollection(usernames);
//        UserId userId = getFirstElementFromCollection(userIds);
//        LoginPasswordId loginPasswordId = getFirstElementFromCollection(loginPasswordIds);
        if (usernameWithPermissions == null ) {
            log.error("One of the required parameters is missing. Login request cannot be processed");
            throw new IllegalStateException();
        }*/
        
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
        OrderDocument order = repository.retrieve(locator);
        if( order == null ) {
            httpServletResponse.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            return null;
        }
        
        // if an order is marked with an owner, then only the owner can cancel it
        if( order.getMeta() != null && order.getMeta().containsKey("owner") ) {
            String subject = getSubjectIdentity();
            log.debug("Existing order with owner: {} vs. subject: {}", order.getMeta().get("owner"), subject);
            if( subject == null || !subject.equals(order.getMeta().get("owner"))) {
                httpServletResponse.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                return null;
            }
        }
        
        
        OrderDispatchQueue.cancelOrder(locator.id.toString());
        order.setStatus("CANCELLING");
        return OrderUtils.sanitize(order);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public OrderDocument retrieveOne(@BeanParam OrderLocator locator, @Context HttpServletRequest httpServletRequest, @Context  HttpServletResponse httpServletResponse) {
        OrderDocument order = super.retrieveOne(locator, httpServletRequest, httpServletResponse);
        if( order == null ) { return null; }
        
        // if an order is marked with an owner, then only the owner can retrieve it
        if( order.getMeta() != null && order.getMeta().containsKey("owner") ) {
            String subject = getSubjectIdentity();
            log.debug("Existing order with owner: {} vs. subject: {}", order.getMeta().get("owner"), subject);
            if( subject == null || !subject.equals(order.getMeta().get("owner"))) {
                httpServletResponse.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                return null;
            }
        }
        
        return OrderUtils.sanitize(order);
    }

    @GET
    @Path("{id}/export")
    @Produces(MediaType.APPLICATION_JSON)
    public OrderDocument exportOrder(@BeanParam OrderLocator locator, @Context HttpServletRequest httpServletRequest, @Context  HttpServletResponse httpServletResponse) {
        OrderDocument order = super.retrieveOne(locator, httpServletRequest, httpServletResponse);
        if( order == null ) { return null; }
        
        
        // if an order is marked with an owner, then only the owner can export it
        if( order.getMeta() != null && order.getMeta().containsKey("owner") ) {
            if( locator.token != null ) {
                Subject currentUser = SecurityUtils.getSubject();
                Token token = new Token(locator.token);
                TokenAuthenticationToken tokenAuthenticationToken = new TokenAuthenticationToken(token, "127.0.0.1");
                currentUser.login(tokenAuthenticationToken); // throws UnknownAccountException , IncorrectCredentialsException , LockedAccountException , other specific exceptions, and AuthenticationException 
                log.debug("Logged in user with token: {}", locator.token);
            }
            String subject = getSubjectIdentity();
            log.debug("Existing order with owner: {} vs. subject: {}", order.getMeta().get("owner"), subject);
            if( subject == null || !subject.equals(order.getMeta().get("owner"))) {
                httpServletResponse.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
                return null;
            }
        }
        
        OrderDocument clean = OrderUtils.sanitize(order);
        if( "DONE".equals(clean.getStatus()) ) {
            // we don't export task status for completed orders
            clean.setProgress(null);
            clean.setProgressMax(null);
            clean.setTasks(null); 
        }
        return clean;
    }
    
    /**
     * This method intentionally disabled to prevent anonymous users
     * from enumerating existing orders and then retrieving generated
     * CIT service credentials.
     * 
     * @param criteria
     * @return 
     */
    @GET
    @Produces(DataMediaType.APPLICATION_VND_API_JSON)
    @Override
    public OrderDocumentCollection searchJsonapiCollection(@BeanParam OrderFilterCriteria criteria) {
        throw new UnsupportedOperationException();
        /*
        OrderDocumentCollection collection = super.searchJsonapiCollection(criteria);
        return OrderUtils.sanitizeCollection(collection);
        */
    }

    /**
     * This method intentionally disabled to prevent anonymous users
     * from enumerating existing orders and then retrieving generated
     * CIT service credentials.
     * 
     * @param selector
     * @return 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public OrderDocumentCollection searchCollection(OrderFilterCriteria selector) {
        throw new UnsupportedOperationException();
        /*
        OrderDocumentCollection collection = super.searchCollection(selector);
        return OrderUtils.sanitizeCollection(collection);
        */
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
