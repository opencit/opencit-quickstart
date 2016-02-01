/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.task.PreconfigureAttestationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.faults.FileNotCreated;
import com.intel.mtwilson.deployment.jaxrs.faults.FileNotFound;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.stringtemplate.v4.ST;
import static org.junit.Assert.*;

/**
 *
 * @author jbuhacoff
 */
public class StringTemplateTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StringTemplateTest.class);

    private String getTemplateFromResources(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            return IOUtils.toString(in, "UTF-8");
        }
    }

    /**
     * prototype for an object to provide data for filling the template
     *
     * @param template
     */
    private Map<String, Object> getData() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("MTWILSON_HOST", "198.51.100.87");
        data.put("MTWILSON_PORT", "8443");
        data.put("MTWILSON_ADMIN_PASSWORD", "user-input-or-generated");
        data.put("MTWILSON_PRIVACYCA_DOWNLOAD_PASSWORD", "generated");
        data.put("DATABASE_PASSWORD", "user-input-for-shared-db-or-generated");

        // optional;  default value should already be tagadmin during install
        data.put("MTWILSON_TAG_ADMIN_USERNAME", "tagadmin");
        data.put("MTWILSON_TAG_ADMIN_PASSWORD", "generated");

        return data;
    }

    @Test
    public void testReadAndFillTemplate() throws IOException {
        String templateSource = getTemplateFromResources("/mtwilson.st");
        ST template = new ST(templateSource, '<', '>');

        // data to fill the template
        Map<String, Object> data = getData();

        for (Entry<String, Object> entry : data.entrySet()) {
            template.add(entry.getKey(), entry.getValue());
        }

        // generate .env file
        log.debug("OUTPUT: {}", template.render());
    }

    
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
        target2.getPackages().add("trustagent_ubuntu");
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
    @Test
    public void generateEnvFileAndCopyToAttestationService() throws IOException {
        SoftwarePackageRepository repository = SoftwarePackageRepositoryFactory.getInstance();
        OrderDocument order = createOrderDocumentB();
        PreconfigureAttestationService task = new PreconfigureAttestationService();
        task.setOrderDocument(order);
        task.setSoftwarePackageRepository(repository);
        task.setTarget(order.getTargets().iterator().next());
        task.run();
        ObjectMapper mapper = new ObjectMapper();
        log.debug("faults? {}", mapper.writeValueAsString(task.getFaults()));
        assertTrue( task.getFaults().isEmpty() );
        
        // look for the output mtwilson.env file:
        File output = new File(Folders.repository("tasks") + File.separator + task.getId() + File.separator + "mtwilson.env");
        String outputText = FileUtils.readFileToString(output, "UTF-8");
        log.debug("Generated: {}", outputText);
    }
}
