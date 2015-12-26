/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.jaxrs2.server.resource.DocumentRepository;
import com.intel.mtwilson.repository.RepositoryCreateConflictException;
import com.intel.mtwilson.repository.RepositoryCreateException;
import com.intel.mtwilson.repository.RepositoryRetrieveException;
import com.intel.mtwilson.repository.RepositoryStoreException;
import com.intel.mtwilson.task.JsonFileRepository;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class OrderDocumentRepository implements DocumentRepository<OrderDocument, OrderDocumentCollection, OrderFilterCriteria, OrderLocator> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDocumentRepository.class);
    private JsonFileRepository json;

    public OrderDocumentRepository() {
        super();
        File directory = new File(Folders.repository("orders"));
        try {
            json = new JsonFileRepository(directory);
        } catch (FileNotFoundException e) {
            log.error("Cannot create repository directory: {}", directory.getAbsolutePath(), e);
            json = null;
        }
    }

    /**
     * Currently supports searching only by order id.
     *
     * @param criteria
     * @return a OrderCollection instance; may be empty if no matches found but
     * will never be null
     */
    @Override
    public OrderDocumentCollection search(OrderFilterCriteria criteria) {
        OrderDocumentCollection results = new OrderDocumentCollection();
        if (criteria != null && criteria.id != null) {
            try {
                OrderDocument found = json.retrieve(criteria.id.toString(), OrderDocument.class);
                if (found != null) {
                    results.getOrders().add(found);
                    return results;
                }
            } catch (Exception e) {
                log.error("Cannot retrieve order: {}", criteria.id.toString(), e);
            }
        }
        else if (criteria != null && !criteria.filter) {
            List<String> orderIds = json.list();
            for(String orderId : orderIds) {
                try {
                OrderDocument found = json.retrieve(orderId, OrderDocument.class);
                if (found != null) {
                    results.getOrders().add(found);
                }
            } catch (Exception e) {
                log.error("Cannot retrieve order: {}", orderId, e);
            }
               
                
            }
        }
        return results;
    }

    /**
     * Searches for orders matching the specified criteria and deletes them.
     *
     * @param criteria
     */
    @Override
    public void delete(OrderFilterCriteria criteria) {
        OrderDocumentCollection ordersToDelete = search(criteria);
        for (OrderDocument orderToDelete : ordersToDelete.getOrders()) {
            json.remove(orderToDelete.getId().toString());
        }
    }

    /**
     *
     * @param locator
     * @return order instance or null if order was not found
     */
    @Override
    public OrderDocument retrieve(OrderLocator locator) {
        if (locator == null || locator.id == null || !json.contains(locator.id.toString())) {
            return null;
        }
        try {
            return json.retrieve(locator.id.toString(), OrderDocument.class);
        } catch (IOException e) {
            log.error("Cannot retrieve order: {}", locator.id.toString(), e);
            throw new RepositoryRetrieveException(e);
        }
    }

    @Override
    public void store(OrderDocument item) {
        if (item == null || item.getId() == null) {
            throw new RepositoryStoreException();
        }
        try {
            json.store(item);
        } catch (IOException e) {
            log.error("Cannot store order: {}", item.getId().toString(), e);
            throw new RepositoryStoreException(e);
        }
    }

    @Override
    public void create(OrderDocument item) {
        if (item == null || item.getId() == null) {
            throw new RepositoryCreateException();
        }
        if (json.contains(item.getId().toString())) {
            throw new RepositoryCreateConflictException();
        }
        try {
            json.create(item);
        } catch (IOException e) {
            log.error("Cannot store order: {}", item.getId().toString(), e);
            throw new RepositoryCreateException(e);
        }
    }

    @Override
    public void delete(OrderLocator locator) {
        OrderDocument orderToDelete = retrieve(locator);
        if (orderToDelete != null) {
            UUID uuid = orderToDelete.getId();
            json.remove(uuid.toString());
        }
    }
}
