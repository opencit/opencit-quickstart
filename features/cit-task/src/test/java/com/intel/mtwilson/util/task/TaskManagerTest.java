/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.dcsg.cpg.performance.AlarmClock;
import com.intel.dcsg.cpg.performance.ProgressMonitor;
import com.intel.dcsg.cpg.validation.Fault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class TaskManagerTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskManagerTest.class);

    private Collection<Task> createTasks() {
        // prepare shuffled collection of tasks with dependencies
        HelloName a = new HelloName("a");
        HelloName b = new HelloName("b");
        HelloName c = new HelloName("c");
        HelloName d = new HelloName("d");
        HelloName e = new HelloName("e");
        a.getDependencies().add(b);
        a.getDependencies().add(c);
        b.getDependencies().add(d);
        c.getDependencies().add(d);
        c.getDependencies().add(e);
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.addAll(Arrays.asList(a, b, c, d, e));
        Collections.shuffle(tasks);
        return tasks;
    }
    
    /**
     * This method tests that, given a collection of tasks with dependencies,
     * task manager executes dependent tasks after their dependencies.
     */
    @Test
    public void testTaskManagerDependencyOrder() {
        Collection<Task> tasks = createTasks();
        // init task manager
        TaskManager taskManager = new TaskManager();
        taskManager.getTasks().addAll(tasks);
        // run tasks
        taskManager.run(); // for example: e, d, c, b, a
    }
    
    @Test
    public void testTaskManagerProgressMonitor() {
        Collection<Task> tasks = createTasks();
        // init task manager
        TaskManager taskManager = new TaskManager();
        taskManager.getTasks().addAll(tasks);
        // run tasks with progress monitor
        ProgressMonitor monitor = new ProgressMonitor(taskManager);
        monitor.start();
        taskManager.run(); // for example: e, d, c, b, a
        monitor.stop();
    }

    /**
     * Tests the ProgressMonitoringTask with the TaskMonitor, which implements
     * Progres because it extends from AbstractTask.
     * The ProgressMonitoringTask just allows more concise code to do the
     * same thing that is demonstrated in testTaskManagerProgressMonitor()
     */
    @Test
    public void testProgressMonitoringTaskWithProgressReportingTaskManager() {
        Collection<Task> tasks = createTasks();
        // init task manager
        TaskManager taskManager = new TaskManager();
        taskManager.getTasks().addAll(tasks);
        // run tasks with progress monitor
        ProgressMonitoringTask task = new ProgressMonitoringTask(taskManager);
        task.run();
    }
    
    /**
     * All tasks extending AbstractTask have a default progress implementation
     * of current=0,max=1 before running and current=1,max=1 after running.
     */
    @Test
    public void testProgressMonitoringTaskWithProgressReportingTask( ){
        Task task = new HelloName("a");
        ProgressMonitoringTask monitoringTask = new ProgressMonitoringTask(task);
        monitoringTask.run();
    }

    /**
     * Tasks not extending AbstractTask may implement Progress themselves or
     * not at all.  In case they do not implement Progress, the ProgressMonitoringTask
     * provides a default implementation similar to that provided by AbstractTask:
     * current=0,max=1 before running and current=1,max=1 after running.
     */
    @Test
    public void testProgressMonitoringTaskWithNonProgressReportingTask( ){
        Task task = new NonProgressReportingTask();
        ProgressMonitoringTask monitoringTask = new ProgressMonitoringTask(task);
        monitoringTask.run();
    }
    
    public static class HelloName extends AbstractTask {
        private String name;
        private AlarmClock clock;

        public HelloName(String name) {
            this.name = name;
            this.clock = new AlarmClock();            
        }

        @Override
        public void execute() {
            clock.sleep(1000); // ms
            log.debug("hello: {}", name); 
        }

        @Override
        public String toString() {
            return name;
        }
        
        
    }
    public static class HelloRandom extends AbstractTask {

        private int id;

        public HelloRandom() {
            id = RandomUtils.nextInt();
        }

        @Override
        public void execute() {
            log.debug("hello: {}", id);
        }
        
        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }
    
    public static class NonProgressReportingTask implements Task {
        private boolean done = false;
        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Collection<Task> getDependencies() {
            return null;
        }

        @Override
        public void run() {
            log.debug("NonProgressReportingTask running");
            done = true;
        }

        @Override
        public Collection<Fault> getFaults() {
            return null;
        }
        
    }
}
