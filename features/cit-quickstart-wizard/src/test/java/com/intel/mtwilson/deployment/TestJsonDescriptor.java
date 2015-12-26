/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.jaxrs.io.TaskDocument;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author jbuhacoff
 */
public class TestJsonDescriptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestJsonDescriptor.class);
    private static ObjectMapper mapper;
    
    @BeforeClass
    public static void initMapper() {
mapper =  JacksonObjectMapperProvider.createDefaultMapper();
mapper.registerModule(new JacksonModule());
        
    }
    /**
     * <pre>
     * {
     * "features":[
     *     {"id":"cit-host-attestation","enabled":null},
     *     {"id":"cit-vm-encryption","enabled":null},
     *     {"id":"cit-vm-attestation","enabled":null}
     * ],
     * "environment":"PRIVATE",
     * "layout":"CUSTOM",
     * "credentials":[
     *     {"host":"192.168.1.101","port":22,"username":"root","password":"password","publicKeyDigest":null},
     *     {"host":"192.168.1.102","port":22,"username":"root","password":"password","publicKeyDigest":null}
     * ],
     * "settings":{
     *     "com.intel.cit.keymanager.kmip":"kmipserver.example.com",
     *     "com.intel.cit.attestation.cache.expires":"3000"
     *     }
     * }
     * </pre>
     * 
     * @return 
     */
    /*
    private OrderDocument createOrderDocumentA() {
        SSH ssh1 = new SSH("192.168.1.101", "password", null);
        SSH ssh2 = new SSH("192.168.1.102", "password", null);
        HashSet<SSH> credentials = new HashSet<>();        
        credentials.add(ssh1);
        credentials.add(ssh2);
        Feature feature1 = new Feature("cit-host-attestation");
        Feature feature2 = new Feature("cit-vm-attestation");
        Feature feature3 = new Feature("cit-vm-encryption");
        HashSet<Feature> features = new HashSet<>();
        features.add(feature1);
        features.add(feature2);
        features.add(feature3);
        HashMap<String,String> settings = new HashMap<>();
        settings.put("com.intel.cit.attestation.cache.expires", "3000");
        settings.put("com.intel.cit.keymanager.kmip", "kmipserver.example.com");
        OrderDocument deployment = new OrderDocument();
        deployment.setCredentials(credentials);
        deployment.setEnvironment(NetworkRole.PRIVATE);
        deployment.setFeatures(features);
        deployment.setLayout(Layout.CUSTOM);
        deployment.setSettings(settings);
        return deployment;
    }
    */
    
    /**
     * 
     * <pre>
     * {
     * "targets":[
     *    {"host":"192.168.1.101","features":[
     *        {"id":"cit-host-attestation","enabled":null},
     *        {"id":"cit-vm-encryption","enabled":null},
     *        {"id":"cit-vm-attestation","enabled":null}
     *         ]
     *    }
     * ],
     * "credentials":[
     *    {"host":"192.168.1.101","port":22,"username":"root","password":"password","publicKeyDigest":null},
     *    {"host":"192.168.1.102","port":22,"username":"root","password":"password","publicKeyDigest":null}
     * ],
     * "settings":{
     *     "com.intel.cit.keymanager.kmip":"kmipserver.example.com",
     *     "com.intel.cit.attestation.cache.expires":"3000"
     *     }
     * }
     * </pre>
     * 
     * @return 
     */
    /*
    private Deployment createDeploymentA() {
        SSH ssh1 = new SSH("192.168.1.101", "password", null);
        SSH ssh2 = new SSH("192.168.1.102", "password", null);
        HashSet<SSH> credentials = new HashSet<>();        
        credentials.add(ssh1);
        credentials.add(ssh2);
        HashMap<String,String> settings = new HashMap<>();
        settings.put("com.intel.cit.attestation.cache.expires", "3000");
        settings.put("com.intel.cit.keymanager.kmip", "kmipserver.example.com");
        Feature feature1 = new Feature("cit-host-attestation");
        Feature feature2 = new Feature("cit-vm-attestation");
        Feature feature3 = new Feature("cit-vm-encryption");
        HashSet<Feature> features = new HashSet<>();
        features.add(feature1);
        features.add(feature2);
        features.add(feature3);
        Target target1 = new Target("192.168.1.101");
        target1.setFeatures(features);
        Target target2 = new Target("192.168.1.101");
        target2.setFeatures(features);
        HashSet<Target> targets = new HashSet<>();
        targets.add(target1);
        targets.add(target2);
        Deployment deployment = new Deployment();
        deployment.setCredentials(credentials);
        deployment.setSettings(settings);
        deployment.setTargets(targets);
        return deployment;
    }
    */
    
    /**
     * <pre>
{
   "features":[
      "attestation_vm",
      "attestation_host",
      "attestation_host_xm"
   ],
   "targets":[
      {
         "host":"198.51.100.14",
         "port":22,
         "username":"root",
         "password":"password",
         "publicKeyDigest":null,
         "timeout":15000,
         "packages":[
            "trust_agent"
         ],
         "networkRole":null
      },
      {
         "host":"198.51.100.8",
         "port":22,
         "username":"root",
         "password":"password",
         "publicKeyDigest":null,
         "timeout":15000,
         "packages":[
            "attestation_service"
         ],
         "networkRole":null
      }
   ],
   "settings":{
      "com.intel.cit.keymanager.kmip":"kmipserver.example.com",
      "com.intel.cit.attestation.cache.expires":"3000"
   }
}
     * </pre>
     * 
     * @return 
     */
    private OrderDocument createOrderDocumentB() {
        Target target1 = new Target();
        target1.setHost("198.51.100.8");
        target1.setPassword("password");
        target1.setPackages(new HashSet<String>());
        target1.getPackages().add("attestation_service");
        Target target2 = new Target();
        target2.setHost("198.51.100.14");
        target2.setPassword("password");
        target2.setPackages(new HashSet<String>());
        target2.getPackages().add("trust_agent");
        HashMap<String,String> settings = new HashMap<>();
        settings.put("com.intel.cit.attestation.cache.expires", "3000");
        settings.put("com.intel.cit.keymanager.kmip", "kmipserver.example.com");
        HashSet<String> features = new HashSet<>();
        features.add("attestation_host");
        features.add("attestation_host_xm");
        features.add("attestation_vm");       
        OrderDocument order = new OrderDocument();
        order.setId(new UUID());
        order.setFeatures(features);
        order.setTargets(new HashSet<Target>());
        order.getTargets().add(target1);
        order.getTargets().add(target2);
        order.setSettings(settings);
        return order;
    }
    
    private void addExampleTaskStatusToOrderDocument(OrderDocument order) {
        Map<String,TaskDocument> map = order.getTaskMap();
        assert map != null;
        
        TaskDocument t1 = new TaskDocument();
        t1.setId(new UUID());
        t1.setName("ExampleTask");
        t1.setProgress(2);
        t1.setProgressMax(8);        
        map.put(t1.getId().toString(), t1);

        TaskDocument t2 = new TaskDocument();
        t2.setId(new UUID());
        t2.setName("ExampleTask2");
        t2.setProgress(7);
        t2.setProgressMax(10);        
        map.put(t2.getId().toString(), t2);
        
        // overall progress
        order.setProgress(1L);
        order.setProgressMax(5L);
    }
    
    @Test
    public void testJsonSerializeInputOrderDocument() throws JsonProcessingException {
        OrderDocument request = createOrderDocumentB();
        log.debug("requestB: {}", mapper.writeValueAsString(request));
    }
    
    @Test
    public void testJsonSerializeOutputOrderDocument() throws JsonProcessingException {
        OrderDocument request = createOrderDocumentB();
        addExampleTaskStatusToOrderDocument(request);
        
        log.debug("requestB: {}", mapper.writeValueAsString(request));
    }
    
    @Test
    public void testJsonSerializeOutputOrderDocumentWithFaults() throws JsonProcessingException {
        OrderDocument request = createOrderDocumentB();
        request.getFaults().add(new Fault("test"));
        
        log.debug("requestB: {}", mapper.writeValueAsString(request));
    }
    
    /**
03:44:06.699 [main] DEBUG c.i.m.deployment.TestJsonDescriptor - input hashcode; -294597489
03:44:06.704 [main] DEBUG c.i.m.deployment.TestJsonDescriptor - reference hashcode; -294597489
03:31:19.304 [main] DEBUG c.i.m.deployment.OrderDocument - equalFeatures? true
03:31:19.304 [main] DEBUG c.i.m.deployment.OrderDocument - equalTargets? true
03:31:19.304 [main] DEBUG c.i.m.deployment.OrderDocument - equalSettings? true     * 
     * @throws IOException 
     */
    @Test
    public void testJsonDeserializeOrderDocument() throws IOException {
//        String filename = "/deployment_request_B.json";
        String filename = "/order1.json";
        try(InputStream in = getClass().getResourceAsStream(filename)) {
            OrderDocument input = mapper.readValue(in, OrderDocument.class);
            OrderDocument reference = createOrderDocumentB();
            log.debug("input hashcode; {}",  input.hashCode());
            log.debug("reference hashcode; {}",  reference.hashCode());
            assertTrue(reference.equals(input)); // features, targets, and settings all equal ,  not clear why this isn't wking
        }
    }
    
    @Test
    public void testJsonDeserializeOrderDocument2WithFaultModule() throws IOException {
        String filename = "/order4.json";
        try(InputStream in = getClass().getResourceAsStream(filename)) {
            OrderDocument input = mapper.readValue(in, OrderDocument.class);
            OrderDocument reference = createOrderDocumentB();
//            assertTrue(reference.equals(input)); // features, targets, and settings all equal ,  not clear why this isn't wking
            
            // add fault to input
            input.getFaults().add(new Fault("hello xyz world"));
            log.debug("Updated order: {}", mapper.writeValueAsString(input));
        }
    }
    
    /*
    @Test
    public void testJsonSerializeDeployment() throws JsonProcessingException {
        Deployment request = createDeploymentA();
        ObjectMapper mapper = new ObjectMapper();
        log.debug("deploymentA: {}", mapper.writeValueAsString(request));
    }
    
    @Test
    public void testJsonDeserializeDeployment() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/deployment_A.json")) {
        ObjectMapper mapper = new ObjectMapper();
            Deployment input = mapper.readValue(in, Deployment.class);
            Deployment reference = createDeploymentA();
            assertEquals(input, reference);
        }
    }
    */
    
}
