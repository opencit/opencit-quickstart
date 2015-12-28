/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.configuration.Configurable;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.performance.CountingIterator;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.task.faults.ExecutionCancelled;
import com.intel.mtwilson.util.task.faults.ExecutionFailed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * For the purpose of scheduling tasks and continuing from one task to the next
 * only when ready, the Task interface includes isDone() and getDependencies()
 * in addition to extending Faults which includes getFaults().
 *
 * A task will normally be executed after its dependencies if all the
 * dependencies report true for isDone() and empty or null for getFaults().
 *
 * @author jbuhacoff
 */
public class TaskManager extends AbstractTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskManager.class);
    private static final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper(); // for debugging only
    private final ArrayList<Task> tasks = new ArrayList<>();
    private boolean cancel = false;

    public TaskManager() {
        super();
    }
    
    public TaskManager(List<Task> tasks) {
        super();
        this.tasks.addAll(tasks);
        DependenciesUtil.sort(this.tasks);
    }
    
    
    @Override
    public void execute() {
        // configure each task that is configurable
        Configuration configuration = getConfiguration();
        for (Task task : tasks) {
            if (task instanceof Configurable) {
                Configurable configurable = (Configurable) task;
                configurable.configure(configuration);
            }
        }
        // run each task and update progress, continuing only if the task
        // completed successfully
        max(tasks.size());
        CountingIterator<Task> it = new CountingIterator(tasks.iterator());
        while (it.hasNext()) {
            if( cancel ) { break; }
            Task task = it.next(); // counter increments here
            try {
                task.run();
                // we only record counter value as progres after task is run 
                // because it represents "completed" without errors
                Collection<Fault> faults = task.getFaults();
                if( faults == null || faults.isEmpty() ) {
                    current(it.getValue());
                }
                else {
                    log.debug("Task {} execution faults: {}", task.getClass().getName(), faults.size());
                    for(Fault fault : faults) {
                        log.debug("Fault {}: {}", fault.getClass().getName(), fault.getDescription());
                        log.debug("Fault: {}", mapper.writeValueAsString(fault));
                    }
                    break;
                }
            } catch (Exception e) {
                log.error("Task run exception", e);
                fault(new ExecutionFailed(e));
                break;
            }
            log.debug("Task manager completed {} tasks", it.getValue());
        }

    }

    /**
     * The task collection can be cleared and modified prior to running the task
     * manager, but must not be modified while task manager is running.
     *
     * @return mutable collection of tasks to be executed
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }
    
    public void setTasks(List<Task> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
        // sort the tasks to ensure any dependencies are executed before their dependent tasks
        DependenciesUtil.sort(this.tasks);
    }
    
    public void cancel() {
        cancel = true;
        fault(new ExecutionCancelled());
    }
    
    public boolean isCancelled() {
        return cancel;
    }
}
