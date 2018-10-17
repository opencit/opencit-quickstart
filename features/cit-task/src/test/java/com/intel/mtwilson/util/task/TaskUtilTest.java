/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.dcsg.cpg.performance.AlarmClock;
import com.intel.dcsg.cpg.validation.Fault;
import java.util.Collection;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jbuhacoff
 */
public class TaskUtilTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskUtilTest.class);

    @Test
    public void testUnwrapDecorator() {
        Delay task = new Delay();
        TaskHelloDecorator decorator1 = new TaskHelloDecorator(task);
        TaskGoodbyeDecorator decorator2 = new TaskGoodbyeDecorator(decorator1);
        
        assertEquals(decorator2, TaskUtil.unwrap(TaskGoodbyeDecorator.class, decorator2));
        assertEquals(decorator1, TaskUtil.unwrap(TaskHelloDecorator.class, decorator2));
        assertEquals(task, TaskUtil.unwrap(Delay.class, decorator2));
        assertNull(TaskUtil.unwrap(TaskHelloDecorator.class, task));
        assertNull(TaskUtil.unwrap(TaskGoodbyeDecorator.class, task));
        assertNull(TaskUtil.unwrap(TaskGoodbyeDecorator.class, decorator1));
    }
    

    public static class Delay extends AbstractTask {

        @Override
        public void execute() {
            AlarmClock alarm = new AlarmClock();
            alarm.sleep(RandomUtils.nextInt(1000));
        }
        
    }

    public static class TaskHelloDecorator implements TaskDecorator {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskHelloDecorator.class);
        private Task delegate;

        public TaskHelloDecorator(Task delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Task getDelegate() {
            return delegate;
        }

        @Override
        public void run() {
            log.debug("Hello");
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public Collection<Task> getDependencies() {
            return delegate.getDependencies();
        }

        @Override
        public Collection<Fault> getFaults() {
            return delegate.getFaults();
        }
        
    }

    
    public static class TaskGoodbyeDecorator implements TaskDecorator {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskHelloDecorator.class);
        private Task delegate;

        public TaskGoodbyeDecorator(Task delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Task getDelegate() {
            return delegate;
        }

        @Override
        public void run() {
            log.debug("Goodbye");
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public Collection<Task> getDependencies() {
            return delegate.getDependencies();
        }

        @Override
        public Collection<Fault> getFaults() {
            return delegate.getFaults();
        }
        
    }    
}
