/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.threads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.performance.BackgroundThread;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.deployment.Id;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentRepository;
import com.intel.mtwilson.deployment.jaxrs.io.TaskDocument;
import com.intel.mtwilson.deployment.threads.OrderDocumentUpdateQueue.OrderStatusUpdate;
import com.intel.mtwilson.deployment.wizard.DeploymentTaskFactory;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.task.DependencyComparator;
import com.intel.mtwilson.util.task.Task;
import com.intel.mtwilson.util.task.TaskManager;
import com.intel.mtwilson.util.validation.faults.Thrown;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * APIs that need to submit new orders for processing should submit through this
 * thread so we can control the use of available server resources.
 *
 * The run() method of this class is invoked periodically by BackgroundThread.
 *
 * @author jbuhacoff
 */
@WebListener
public class OrderDispatchQueue implements ServletContextListener {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDispatchQueue.class);
    private static final ConcurrentLinkedQueue<OrderDocument> dispatchQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<String, OrderDispatch> currentOrders = new ConcurrentHashMap<>();
    private static final BackgroundThread dispatchThread = new BackgroundThread();
//    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("OrderDispatchQueue contextInitialized");
        // start a background thread to periodically get orders from the
        // queue and start the processing in a new thread
        try {
            Configuration configuration = ConfigurationFactory.getConfiguration();
            dispatchThread.setDelay(Long.valueOf(configuration.get("mtwilson.quickstart.order.dispatch.interval", "200")).longValue(), TimeUnit.MILLISECONDS);
//            scheduler.scheduleWithFixedDelay(new OrderDispatchPeriodicTask(), 1, Long.valueOf(configuration.get("mtwilson.quickstart.order.dispatch.interval", "200")).longValue(), TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            log.error("Cannot load configuration, using default period 200ms", e);
            dispatchThread.setDelay(200, TimeUnit.MILLISECONDS);
        }
        dispatchThread.setTask(new OrderDispatchPeriodicTask());
        dispatchThread.start();
        log.debug("OrderDispatchQueue started dispatch thread");
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("OrderDispatchQueue contextInitialized");
        dispatchThread.stop();
        executor.shutdownNow();
    }

    /**
     * Dispatching an order means creating a task manager, a thread to run the
     * task manager, and another thread to monitor the progress and update the
     * OrderDocument asynchronously
     */
    public static class OrderDispatchPeriodicTask implements Runnable {
        
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDispatchPeriodicTask.class);
        private final OrderDocumentRepository repository = new OrderDocumentRepository();
        private final PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy namingStrategy = new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy();
        private final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper(); // for debug only

        @Override
        public void run() {
//            log.debug("OrderDispatchPeriodicTask run()");
            OrderDocument nextOrder = dispatchQueue.poll();
            while (nextOrder != null) {
                String orderId = nextOrder.getId().toString();

                // generate the tasks that will execute the order;
                // this includes input validation on selected features and software packages
                DeploymentTaskFactory taskFactory;
                try {
                    taskFactory = new DeploymentTaskFactory(nextOrder);
                    taskFactory.run();

                    // create the task manager for executing the order
                    List<Task> generatedTasks = taskFactory.getOutput();
                    
                    
                    if (generatedTasks != null && !generatedTasks.isEmpty()) {
                        TaskManager taskManager = new TaskManager(generatedTasks);
                        nextOrder.setTasks(createTaskDocuments(taskManager.getTasks()));
                        
                        if (log.isDebugEnabled()) {
                            log.debug("order document with tasks: {}", mapper.writeValueAsString(nextOrder));
                        }

                        // store the order again, this time with task-specific data from createTaskDocuments()
                        repository.store(nextOrder);
                        
                        log.debug("Submitting new order for execution: {}", orderId);
                        Future<String> future = executor.submit(taskManager, orderId);
                        OrderDispatch dispatch = new OrderDispatch(nextOrder, taskManager, future);
                        currentOrders.put(orderId, dispatch);
                        log.debug("Added new order to current orders map: {}", orderId);
                        
                        OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(nextOrder.getId(), "ACTIVE", 0L, Integer.valueOf(generatedTasks.size()).longValue()));
                    } else {
                        log.error("TaskManager did not generate tasks for this order: {}", orderId);
                        ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
                        log.debug("DeploymentTaskFactory faults: {}", mapper.writeValueAsString(taskFactory));
                        OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(nextOrder.getId(), "ERROR", 0L, Integer.valueOf(generatedTasks.size()).longValue()));
                    }
                    
                } catch (IOException | RuntimeException e) {
                    log.error("Cannot dispatch order", e);
                    nextOrder.getFaults().add(new Thrown(e, "Cannot dispatch order"));
                    repository.store(nextOrder); // store the updated order with the fault
                }
                
                
                nextOrder = dispatchQueue.poll();
            }
        }
        
        private List<TaskDocument> createTaskDocuments(Collection<Task> tasks) {
            ArrayList<TaskDocument> taskDocuments = new ArrayList<>();
            int sequence = 0;
            for (Task task : tasks) {
                sequence++;
                TaskDocument taskDocument = new TaskDocument();
                if (task instanceof Id) {
                    Id taskWithId = (Id) task;
                    taskDocument.setId(UUID.valueOf(taskWithId.getId()));
                } else {
                    log.error("Task class does not implement Id: {}", task.getClass().getName());
                    taskDocument.setId(new UUID());
                }
                taskDocument.setName(task.getClass().getName());
                taskDocument.setSequence(sequence);
                // setting progress to 0/1 because we know we haven't started processing yet...
                // once execution starts, the task progress will be updated via the OrderProgressMonitor and OrderDocumentUpdateQueue threads
                taskDocument.setProgress(0);
                taskDocument.setProgressMax(1);

                // copy other attributes of the task
                try {
                    // note that the attribute keys we get back from PropertyUtils are in camelCase  , like "packageName"  ;  we translate to lowercase_with_underscores later
                    Map<String, Object> attributes = PropertyUtils.describe(task);
                    attributes.remove("id");  // unnecessary overlap with id in top level
                    attributes.remove("current"); // unnecessary overlap with progress in top level
                    attributes.remove("max"); // unnecessary overlap with progressMax in top level
                    attributes.remove("class"); // unnecessary overlap with name in top level
                    attributes.remove("faults"); // undesired raw faults; in top level we have the fault descriptors instead
                    attributes.remove("dependencies"); // undesired possibly large mapping
                    attributes.remove("preconditions");// undesired possibly large mapping
                    attributes.remove("postconditions");// undesired possibly large mapping
                    attributes.remove("configuration");// undesired possibly large mapping
                    // transform keys from camelCase to lowercase_with_underscores to match jackson output
                    HashMap<String, Object> data = new HashMap<>();
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                        log.debug("extra data for task document: {} = {}", entry.getKey(), entry.getValue());
                        data.put(namingStrategy.translate(entry.getKey()), entry.getValue());
                    }
                    taskDocument.setData(data);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    log.error("Cannot map attributes of task to task document", e);
                    // but this is not a fatal error, so we just move on.
                }

                // add a link to an API that will show any output from the task
                // such as logs
                taskDocument.getLinks().put("output", "/v1/quickstart/tasks/" + taskDocument.getId().toString() + "/output");
                
                taskDocuments.add(taskDocument);
            }
            return taskDocuments;
        }
    }

    /**
     * Example:
     * <pre>
     * OrderProcessingQueue.getProcessQueue().add(orderDocument);
     * </pre>
     *
     * @return the thread-safe queue in which to place order document updates
     */
    public static ConcurrentLinkedQueue<OrderDocument> getDispatchQueue() {
        return dispatchQueue;
    }
    
    public static ConcurrentHashMap<String, OrderDispatch> getCurrentOrders() {
        return currentOrders;
    }
    
    public static void cancelOrder(String orderId) {
        OrderDispatch dispatch = currentOrders.get(orderId);
        if (dispatch == null) {
            log.debug("Cannot cancel order because not currently running: {}", orderId);
            return;
        }

        // first attempt a "graceful" termination by informing the 
        // task manager of that order that we need to exit early
        dispatch.getTaskManager().cancel();

        // the thread that is processing this order can be cancelled via the
        // Future object that was returned by the Executor
        boolean result = dispatch.getFuture().cancel(true);
        log.debug("Order cancelled? {}", result);

        // remove the order from our map
        currentOrders.remove(orderId);

        // submit an update request to the order record
        OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(dispatch.getOrderDocument().getId(), "CANCELLED", null, null));
    }
    
    public static class OrderDispatch {
        
        private OrderDocument orderDocument;
        private TaskManager taskManager;
        private Future<String> future;
        
        public OrderDispatch(OrderDocument orderDocument, TaskManager taskManager, Future<String> future) {
            this.orderDocument = orderDocument;
            this.taskManager = taskManager;
            this.future = future;
        }
        
        public OrderDocument getOrderDocument() {
            return orderDocument;
        }
        
        public TaskManager getTaskManager() {
            return taskManager;
        }
        
        public Future<String> getFuture() {
            return future;
        }
    }
}
