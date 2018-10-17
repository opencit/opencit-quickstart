/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.deployment.jaxrs.io.TaskDocument;
import com.intel.mtwilson.deployment.task.AbstractTaskWithId;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class CreateTaskDocumentTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateTaskDocumentTest.class);
    private static final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
    private static final PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy namingStrategy = new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy();

    /**
     * Example output:
     * 
     * Original Task:
     * <pre>
     * {
     * "faults":[],
     * "dependencies":[],
     * "preconditions":[],
     * "postconditions":[],
     * "configuration":{"properties":{},"editable":true},
     * "current":0,
     * "max":0,
     * "active":false,
     * "done":false,
     * "id":"599164b0-4867-45dd-af41-f26ba93a4252",
     * "package_name":"baz",
     * "bar":5,
     * "foo":"foo"
     * }
     * </pre>
     * 
     * Resulting Task Document:
     * <pre>
     * {
     * "id":"6a40e518-c846-461a-9c01-8ff317bc3f90",
     * "links":{"output":"/v1/quickstart/tasks/6a40e518-c846-461a-9c01-8ff317bc3f90/output"},
     * "name":"com.intel.mtwilson.deployment.CreateTaskDocumentTest$CustomTask",
     * "progress":0,
     * "progress_max":1
     * "data":{
     *   "max":0,
     *   "packageName":"baz",
     *   "done":false,
     *   "foo":"foo",
     *    "current":0,
     *    "active":false,
     *    "bar":5
     *  }
     * }
     * 
     * 
     * </pre>
     * 
     * @throws JsonProcessingException
     */
    @Test
    public void testCreateTaskDocumentFromTaskWithExtendedAttributes() throws JsonProcessingException {
        CustomTask task = new CustomTask();
            log.debug("original task: {}", mapper.writeValueAsString(task));

        TaskDocument taskDocument = new TaskDocument();
        if (task instanceof Id) {
            Id taskWithId = (Id) task;
            taskDocument.setId(UUID.valueOf(taskWithId.getId()));
        } else {
            log.error("Task class does not implement Id: {}", task.getClass().getName());
            taskDocument.setId(new UUID());
        }
        taskDocument.setName(task.getClass().getName());
        taskDocument.setProgress(0);
        taskDocument.setProgressMax(1);

        // copy other attributes of the task
        try {
            Map<String, Object> attributes = PropertyUtils.describe(task);
            log.debug("described task: {}", mapper.writeValueAsString(attributes));
            attributes.remove("id");  // unnecessary overlap with id in top level
            attributes.remove("current"); // unnecessary overlap with progress in top level
            attributes.remove("max"); // unnecessary overlap with progressMax in top level
            attributes.remove("class"); // unnecessary overlap with name in top level
            attributes.remove("faults"); // undesired raw faults; in top level we have the fault descriptors instead
            attributes.remove("dependencies"); // undesired possibly large mapping
            attributes.remove("preconditions");// undesired possibly large mapping
            attributes.remove("postconditions");// undesired possibly large mapping
            attributes.remove("configuration");// undesired possibly large mapping
            
            taskDocument.setData(attributes);
            HashMap<String,Object> data = new HashMap<>();
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
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

        log.debug("task document: {}", mapper.writeValueAsString(taskDocument));
    }

    public static class CustomTask extends AbstractTaskWithId {

        @Override
        public void execute() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getFoo() {
            return "foo";
        }

        public Integer getBar() {
            return 5;
        }

        public String getPackageName() {
            return "baz";
        }
    }
}
