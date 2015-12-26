/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.wizard.DeploymentTaskFactory;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.task.Task;
import com.intel.mtwilson.util.task.TaskManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class TaskFactoryTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskFactoryTest.class);
    private static final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
    
    private OrderDocument readOrder(String filename) throws IOException {
        try(InputStream in = getClass().getResourceAsStream(filename)) {
            OrderDocument input = mapper.readValue(in, OrderDocument.class);
            return input;
        }
    }
    
    @Test
    public void testCreateTasks() throws IOException {
        OrderDocument order = readOrder("/order4.json");
        
                // generate the tasks that will execute the order;
                // this includes input validation on selected features and software packages
                DeploymentTaskFactory taskFactory = new DeploymentTaskFactory(order);
                taskFactory.run();
                
                // create the task manager for executing the order
                List<Task> generatedTasks = taskFactory.getOutput();
                if( generatedTasks != null && !generatedTasks.isEmpty() ) {
                    log.debug("generated tasks: {}", mapper.writeValueAsString(generatedTasks));
                    TaskManager taskManager = new TaskManager();
                    taskManager.getTasks().addAll(generatedTasks);
                }
                else {
                    log.debug("no generated tasks.  faults: {}", mapper.writeValueAsString(taskFactory.getFaults()));
                }
    }
}
