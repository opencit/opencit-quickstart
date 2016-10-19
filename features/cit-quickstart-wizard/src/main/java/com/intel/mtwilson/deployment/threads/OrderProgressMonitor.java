/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.threads;

import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.performance.BackgroundThread;
import com.intel.dcsg.cpg.performance.Progress;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.deployment.Id;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocumentRepository;
import com.intel.mtwilson.deployment.threads.OrderDispatchQueue.OrderDispatch;
import com.intel.mtwilson.deployment.threads.OrderDocumentUpdateQueue.OrderSettingsUpdate;
import com.intel.mtwilson.deployment.threads.OrderDocumentUpdateQueue.OrderStatusUpdate;
import com.intel.mtwilson.deployment.threads.OrderDocumentUpdateQueue.TaskProgressUpdate;
import com.intel.mtwilson.util.task.Task;
import com.intel.mtwilson.util.task.TaskManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * APIs that need to submit new orders for processing should submit through this
 * thread so we can control the use of available server resources.
 *
 * The run() method of this class is invoked periodically by BackgroundThread.
 *
 * Unlike the OrderDispatchQueue and OrderDocumentUpdateQueue, this background
 * thread does not have a queue for work - it monitors the current orders
 * maintained by the OrderDispatchQueue and generates updates to their
 * corresponding order documents.
 *
 * @author jbuhacoff
 */
@WebListener
public class OrderProgressMonitor implements ServletContextListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderProgressMonitor.class);
    private static final BackgroundThread monitoringThread = new BackgroundThread();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("OrderProgressMonitor contextInitialized");
        // start a background thread to periodically get orders from the
        // queue and start the processing in a new thread
        try {
            Configuration configuration = ConfigurationFactory.getConfiguration();
            monitoringThread.setDelay(Long.valueOf(configuration.get("mtwilson.quickstart.order.monitor.interval", "200")).longValue(), TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            log.error("Cannot load configuration, using default period 200ms", e);
            monitoringThread.setDelay(200, TimeUnit.MILLISECONDS);
        }
        monitoringThread.setTask(new OrderProgressMonitorPeriodicTask());
        monitoringThread.start();
        log.debug("OrderProgressMonitor started monitoring thread");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("OrderProgressMonitor contextDestroyed");
        monitoringThread.stop();
    }

    /**
     * Dispatching an order means creating a task manager, a thread to run the
     * task manager, and another thread to monitor the progress and update the
     * OrderDocument asynchronously
     */
    public static class OrderProgressMonitorPeriodicTask implements Runnable {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderProgressMonitorPeriodicTask.class);
        private OrderDocumentRepository repository = new OrderDocumentRepository();

        @Override
        public void run() {
//            log.debug("OrderProgressMonitorPeriodicTask run()");
            Map<String, OrderDispatch> currentOrders = OrderDispatchQueue.getCurrentOrders();
            if (currentOrders == null) {
                log.error("Cannot monitor progress because current orders map is null");
                return;
            }
            
            // create a list of orders to remove after the loop, in order to
            // avoid concurrent modification of the orders map while iterating
            // on its values in the loop
            ArrayList<String> ordersToRemove = new ArrayList<>();

            // look at each of the current orders and generate order document updates
            for (OrderDispatch orderDispatch : currentOrders.values()) {
//                String status = orderDispatch.getOrderDocument().getStatus();

                TaskManager taskManager = orderDispatch.getTaskManager();
                // update progress on each task
                Collection<Task> tasks = taskManager.getTasks();
                for (Task task : tasks) {
                    long current, max;

                    // implementing Progress is optional, so we have a default
                    // progress of 0/1 or 1/1 for tasks that don't implement it
                    if (task instanceof Progress) {
                        Progress taskWithProgress = (Progress) task;
                        current = taskWithProgress.getCurrent();
                        max = taskWithProgress.getMax();
                    } else if (task.isDone()) {
                        current = 1;
                        max = 1;
                    } else {
                        current = 0;
                        max = 1;
                    }

                    // casting to Id but it's not optional - all tasks
                    // generated by DeploymentTaskFactory must extend
                    // AbstractTaskWithId or implement Id on their own
                    if (task instanceof Id) {
                        Id taskWithId = (Id) task;
                        OrderDocumentUpdateQueue.getUpdateQueue().add(new TaskProgressUpdate(orderDispatch.getOrderDocument().getId(), taskWithId.getId(), task.getClass().getName(), task.isDone(), current, max));
                    } else {
                        log.error("Task class {} does not implement id, cannot update progress", task.getClass().getName());
                    }

                }
                
                // NOTE: the OrderDispatchQueue already handles cancelling tasks including the OrderStatusUpdate with status "CANCELLED"
                if (taskManager.isActive()) {
                    // the task manager itself has the overall progress to report
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(orderDispatch.getOrderDocument().getId(), "ACTIVE", taskManager.getCurrent(), taskManager.getMax()));
                }
                if (taskManager.isDone() && taskManager.getCurrent() == taskManager.getMax() && taskManager.getFaults().isEmpty()) {
                    log.debug("OrderProgressMonitor observed task manager is done");
                    // done (completed successfully): update status and remove the order
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(orderDispatch.getOrderDocument().getId(), "DONE", taskManager.getCurrent(), taskManager.getMax()));
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderSettingsUpdate(orderDispatch.getOrderDocument().getId(), orderDispatch.getOrderDocument().getSettings()));
                    ordersToRemove.add(orderDispatch.getOrderDocument().getId().toString());
                }
                if (!taskManager.isCancelled() && !taskManager.getFaults().isEmpty()) {
                    // run or postcondition error (other than cancelled):  update status to "ERROR" with faults;  we check for not cancelled because if it's cancelled then OrderDispatchQueue is already changing status to "CANCELLED"
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(orderDispatch.getOrderDocument().getId(), "ERROR", taskManager.getCurrent(), taskManager.getMax()));
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(orderDispatch.getOrderDocument().getId(), "ERROR", taskManager.getFaults()));
                }
                if (!taskManager.isActive() && !taskManager.isCancelled() && taskManager.getCurrent() > 0 && taskManager.getCurrent() < taskManager.getMax()) {
                    // it's an error for the task manager to be inactive but not completed all tasks (during postcondition testing, active=false but current=max)
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderStatusUpdate(orderDispatch.getOrderDocument().getId(), "ERROR", taskManager.getCurrent(), taskManager.getMax()));
                    OrderDocumentUpdateQueue.getUpdateQueue().add(new OrderSettingsUpdate(orderDispatch.getOrderDocument().getId(), orderDispatch.getOrderDocument().getSettings()));
                    ordersToRemove.add(orderDispatch.getOrderDocument().getId().toString());
                }
            }

            for(String orderId : ordersToRemove) {
                currentOrders.remove(orderId);
            }

        }
    }
}
