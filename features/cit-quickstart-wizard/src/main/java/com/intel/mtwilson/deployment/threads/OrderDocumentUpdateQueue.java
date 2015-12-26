/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.threads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.performance.BackgroundThread;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentRepository;
import com.intel.mtwilson.deployment.jaxrs.io.OrderLocator;
import com.intel.mtwilson.deployment.jaxrs.io.TaskDocument;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Tasks that need to update the order document should submit the updates through
 * this thread so we can control the rate at which we write those updates.
 * 
 * The run() method of this class is invoked periodically by BackgroundThread.
 * 
 * @author jbuhacoff
 */
@WebListener
public class OrderDocumentUpdateQueue implements ServletContextListener {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDocumentUpdateQueue.class);
    private static final ConcurrentLinkedQueue<OrderDocumentUpdate> queue = new ConcurrentLinkedQueue<>();
    private static final BackgroundThread updateThread = new BackgroundThread();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("OrderDocumentUpdateQueue contextInitialized");
        // start a background thread to periodically get updates from the
        // queue and write them to the repository; multiple updates to the
        // same resource can be combined in memory so we only do a single write
        try {
            Configuration configuration = ConfigurationFactory.getConfiguration();
            updateThread.setDelay(Long.valueOf(configuration.get("mtwilson.quickstart.order.update.interval", "200")).longValue(), TimeUnit.MILLISECONDS);
        }
        catch(IOException e) {
            log.error("Cannot load configuration, using default period 200ms", e);
            updateThread.setDelay(200, TimeUnit.MILLISECONDS);
        }
        updateThread.setTask(new OrderDocumentUpdatePeriodicTask());
        updateThread.start();
        log.debug("OrderDocumentUpdateQueue started update thread");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("OrderDocumentUpdateQueue contextDestroyed");
        updateThread.stop();
    }
    
    public static class OrderDocumentUpdatePeriodicTask implements Runnable {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDocumentUpdatePeriodicTask.class);
        private static final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
        private OrderDocumentRepository repository = new OrderDocumentRepository();
        
        @Override
        public void run() {
//            log.debug("OrderDocumentUpdatePeriodicTask run()");
            HashMap<String,OrderDocument> map = new HashMap<>();
            OrderLocator locator = new OrderLocator();
            OrderDocumentUpdate nextUpdate = queue.poll();
            while(nextUpdate != null) {
                locator.id = nextUpdate.getOrderId();
                String orderId = locator.id.toString();
                OrderDocument order = map.get(orderId);
                if( order == null ) {
                    order = repository.retrieve(locator);
                    if( order == null ) {
                        log.error("Attempt to update a non-existent order failed: {}", locator.id.toString());
                        nextUpdate = queue.poll();
                        continue;
                    }
                    map.put(order.getId().toString(), order);
                }
                // update the order
                try {
                log.debug("Updating order with {}: {}", nextUpdate.getClass().getName(), mapper.writeValueAsString(nextUpdate));
                }
                catch(IOException e) {
                    log.debug("Updating order with {}: (cannot serialize)", nextUpdate.getClass().getName(), e);
                }
                nextUpdate.update(order);
                order.setModifiedOn(new Date());
                nextUpdate = queue.poll();
            }
            // write everything we modified to the repository
            for(OrderDocument value : map.values()) {
                log.debug("Storing updates to order {}", value.getId().toString());
                repository.store(value);
            }
        }
        
    }
    
    /**
     * Example:
     * <pre>
     * OrderDocumentUpdateQueue.getUpdateQueue().add(new TaskProgressUpdate(...));
     * </pre>
     * 
     * @return the  thread-safe queue in which to place order document updates
     */
    public static ConcurrentLinkedQueue<OrderDocumentUpdate> getUpdateQueue() {
        return queue;
    }
    
    public static interface OrderDocumentUpdate {
        UUID getOrderId();
        void update(OrderDocument order);
    }
    public static class TaskProgressUpdate implements OrderDocumentUpdate {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskProgressUpdate.class);
        private UUID orderId;
        private String taskId, name;
        private long progress, progressMax;
        private ArrayList<Fault> faults = new ArrayList<>();

        public TaskProgressUpdate(UUID orderId, String taskId, String name, long progress, long progressMax) {
            this.orderId = orderId;
            this.taskId = taskId;
            this.name = name;
            this.progress = progress;
            this.progressMax = progressMax;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getName() {
            return name;
        }
        
        public long getProgress() {
            return progress;
        }

        public long getProgressMax() {
            return progressMax;
        }

        public ArrayList<Fault> getFaults() {
            return faults;
        }

        @Override
        public void update(OrderDocument order) {
            ArrayList<TaskDocument> tasks = new ArrayList<>();
            
            Collection<TaskDocument> existingTasks = order.getTasks();
            if( existingTasks != null ) { tasks.addAll(existingTasks); }
            // find the task id in the order's tasks
            TaskDocument found = null;
            for(TaskDocument task : tasks) {
                if( task.getId().toString().equals(taskId) ) {
                    found = task;
                    break;
                }
            }
            if( found == null ) {
                log.error("Order document does not have specified task id: {} name: {}", taskId, name);
                found = new TaskDocument();
                found.setId(UUID.valueOf(taskId));
                tasks.add(found);
            }
            found.setName(name);
            found.setProgress(progress);
            found.setProgressMax(progressMax);
            found.getFaults().addAll(faults);
            
            order.setTasks(tasks);
        }

        @Override
        public UUID getOrderId() {
            return orderId;
        }
        
    }
    
    public static class PackageInstalledUpdate implements OrderDocumentUpdate {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PackageInstalledUpdate.class);
        private UUID orderId;
        private String host, packageName;

        public PackageInstalledUpdate(UUID orderId, String host, String packageName) {
            this.orderId = orderId;
            this.host = host;
            this.packageName = packageName;
        }

        @Override
        public void update(OrderDocument order) {
            Set<Target> targets = order.getTargets();
            Target found = null;
            for(Target target : targets) {
                if( target.getHost().equals(host) ) {
                    found = target;
                    break;
                }
            }
            if( found == null ) {
                log.error("Order document does not have specified target: {}", host);
                return;
            }
            Set<String> packagesInstalled = found.getPackagesInstalled();
            if( packagesInstalled == null ) {
                packagesInstalled = new HashSet<>();
                found.setPackagesInstalled(packagesInstalled);
            }
            packagesInstalled.add(packageName);
        }

        public String getHost() {
            return host;
        }

        public String getPackageName() {
            return packageName;
        }

        @Override
        public UUID getOrderId() {
            return orderId;
        }
        
    }
    
    /**
     * Updates status, progress, and progressMax for the entire order. 
     * Status is a keyword like "PENDING", "ACTIVE", etc.
     * If progress is not provided (or null), then it will not be updated. 
     * It's safe to update only the status.  
     */
    public static class OrderStatusUpdate implements OrderDocumentUpdate {
        private UUID orderId;
        private String status;
        private Long progress, progressMax;

        public OrderStatusUpdate(UUID orderId, String status) {
            this.orderId = orderId;
            this.status = status;
            this.progress = null;
            this.progressMax = null;
        }
        public OrderStatusUpdate(UUID orderId, String status, Long progress, Long progressMax) {
            this.orderId = orderId;
            this.status = status;
            this.progress = progress;
            this.progressMax = progressMax;
        }

        public String getStatus() {
            return status;
        }

        public Long getProgress() {
            return progress;
        }

        public Long getProgressMax() {
            return progressMax;
        }
        
        @Override
        public UUID getOrderId() {
            return orderId;
        }

        @Override
        public void update(OrderDocument order) {
            if( status != null ) { order.setStatus(status); }
            if( progress != null ) { order.setProgress(progress); }
            if( progressMax != null ) { order.setProgressMax(progressMax); }
        }
        
    }
}
