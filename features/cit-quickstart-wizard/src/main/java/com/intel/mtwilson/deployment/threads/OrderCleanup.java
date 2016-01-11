/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.threads;

import com.intel.dcsg.cpg.configuration.Configurable;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.console.Command;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.io.file.FileOnlyFilter;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentRepository;
import com.intel.mtwilson.deployment.jaxrs.io.OrderLocator;
import com.intel.mtwilson.deployment.jaxrs.io.TaskDocument;
import com.intel.mtwilson.task.JsonFileRepository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author jbuhacoff
 */
public class OrderCleanup implements Runnable, Configurable, Command {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderCleanup.class);

    private Configuration configuration;

    @Override
    public void run() {
        log.debug("Running OrderCleanup");
        if (configuration == null) {
            configuration = new PropertiesConfiguration();
        }
        long currentTime = System.currentTimeMillis(); // we measure order age relative to when this job started
        long thresholdExpireModified = Long.valueOf(configuration.get("mtwilson.quickstart.order.cleanup.modified", "86400000")).longValue(); // default 24 hours; delete any orders that have not been modified in 24 hours
        long thresholdExpireAccessed = Long.valueOf(configuration.get("mtwilson.quickstart.order.cleanup.accessed", "604800000")).longValue(); //   default 7 days; delete any orders that have not been accessed in 7 days 
        FileTime expireIfModifiedBefore = FileTime.fromMillis(currentTime - thresholdExpireModified);
        FileTime expireIfAccessedBefore = FileTime.fromMillis(currentTime - thresholdExpireAccessed);
        Map<String, OrderDispatchQueue.OrderDispatch> currentOrders = OrderDispatchQueue.getCurrentOrders();
        // list orders in the order directory
        File orderDirectory = new File(Folders.repository("orders"));
        File taskDirectory = new File(Folders.repository("tasks"));
        File[] orderFiles = orderDirectory.listFiles(new FileOnlyFilter());
        for (File orderFile : orderFiles) {
            String orderId = orderFile.getName();
            // if there is an active order with this id, skip it
            if (currentOrders.containsKey(orderId)) {
                log.debug("Skipping active order {}", orderId);
                continue;
            }
            // try reading the file modification and access times
            try {
                BasicFileAttributes attrs = Files.readAttributes(orderFile.toPath(), BasicFileAttributes.class);
                if (attrs.lastModifiedTime().compareTo(expireIfModifiedBefore) > 0) {
                    log.debug("Skipping recently modified order {}", orderId);
                    continue;
                }
                if (attrs.lastAccessTime().compareTo(expireIfAccessedBefore) > 0) {
                    log.debug("Skipping recently accessed order {}", orderId);
                    continue;
                }
            } catch (IOException e) {
                log.error("Skipping order {} because cannot read last accessed time", orderId, e);
                continue;
            }
            // order is expired, delete all its tasks then delete the order itself
            log.debug("Deleting expired order {}", orderId);
            try {
                JsonFileRepository taskRepository = new JsonFileRepository(taskDirectory);
                OrderLocator orderDocumentLocator = new OrderLocator();
                orderDocumentLocator.id = UUID.valueOf(orderId);
                OrderDocumentRepository orderRepository = new OrderDocumentRepository();
                OrderDocument orderDocument = orderRepository.retrieve(orderDocumentLocator);
                // first delete any tasks associated with the order
                Collection<TaskDocument> tasks = orderDocument.getTasks();
                for (TaskDocument task : tasks) {
                    String taskId = task.getId().toString();
                    taskRepository.remove(taskId); // recursively deletes the task directory
                }
                // now delete the order file itself
                orderRepository.delete(orderDocumentLocator);
            } catch (IOException e) {
                log.error("Cannot delete tasks in order {}", orderId, e);
            }
        }
    }

    @Override
    public void configure(Configuration configuration) {
        this.configuration = configuration;
    }

    private org.apache.commons.configuration.Configuration options;
    @Override
    public void setOptions(org.apache.commons.configuration.Configuration options) {
        this.options = options;
    }

    @Override
    public void execute(String[] args) throws Exception {
        OrderCleanup command = new OrderCleanup();
        command.configure(ConfigurationFactory.getConfiguration());
        command.run();
    }
    
}
