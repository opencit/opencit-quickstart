/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.deployment.conditions.EnvironmentAvailable;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class JsonConditionTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonConditionTest.class);
    private static ObjectMapper mapper;
    
    @BeforeClass
    public static void mapper() {
        mapper = JacksonObjectMapperProvider.createDefaultMapper();
        mapper.registerModule(new JacksonModule());
    }
    
    /**
     * With the jackson module that registers the ConditionTypeMixIn:
     * <pre>
     * EnvironmentAvailable: {"@class":"com.intel.mtwilson.deployment.conditions.EnvironmentAvailable"}
     * </pre>
     * 
     * Without the module:
     * <pre>
     * com.fasterxml.jackson.databind.JsonMappingException: No serializer found for class com.intel.mtwilson.deployment.conditions.EnvironmentAvailable and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
     * </pre>
     * 
     * @throws JsonProcessingException 
     */
    @Test
    public void testSerializeEnvironmentAvailable() throws JsonProcessingException {
        EnvironmentAvailable condition = new EnvironmentAvailable("PRIVATE");
        log.debug("EnvironmentAvailable: {}", mapper.writeValueAsString(condition));
    }
}
