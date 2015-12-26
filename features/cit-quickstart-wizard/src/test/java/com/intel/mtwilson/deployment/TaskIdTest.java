/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.util.task.AbstractTask;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class TaskIdTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskIdTest.class);

    @Test
    public void testTaskWithId() {
        ExampleTask example = new ExampleTask();
        log.debug("Example id: {}", example.getId());
        example.run();
    }
    
    public static interface Id {
        String getId();
    }
    
    public static abstract class AbstractTaskWithId extends AbstractTask implements Id {
        private String id;

        public AbstractTaskWithId() {
            super();
            this.id = new UUID().toString();
        }
        
        public AbstractTaskWithId(String id) {
            super();
            this.id = id;
        }
        
        @Override
        public String getId() {
            return id;
        }
        
    }
    
    public static class ExampleTask extends AbstractTaskWithId {

        
        @Override
        public void execute() {
            log.debug("Hello world");
        }
        
    }
}
