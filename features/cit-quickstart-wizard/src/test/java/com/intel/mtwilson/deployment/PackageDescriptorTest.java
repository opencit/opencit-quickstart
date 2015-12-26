/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.descriptor.SoftwarePackageDescriptorCollection;
import com.intel.mtwilson.deployment.descriptor.SoftwarePackageDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.task.Dependencies;
import com.intel.mtwilson.util.task.DependencyComparator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class PackageDescriptorTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PackageDescriptorTest.class);
    private static final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();

    @Test
    public void testReadSoftwareProperties() throws IOException {
        HashMap<String, String> packageFileMap = new HashMap<>();
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/software.properties")) {
            properties.load(in);
            Set<String> packageNames = properties.stringPropertyNames();
            for (String packageName : packageNames) {
                packageFileMap.put(packageName, properties.getProperty(packageName));
            }
        }

        log.debug("software packages: {}", mapper.writeValueAsString(packageFileMap));
    }

    @Test
    public void testReadSoftwareJson() throws IOException {
        SoftwarePackageDescriptorCollection descriptorCollection = mapper.readValue(getClass().getResourceAsStream("/software-packages.json"), SoftwarePackageDescriptorCollection.class);

        log.debug("software packages: {}", mapper.writeValueAsString(descriptorCollection));
    }



    @Test
    public void testSoftwarePackageFactory() throws IOException {
        JsonSoftwarePackageRepository softwarePackageRepository = new JsonSoftwarePackageRepository(getClass().getResourceAsStream("/software-packages.json"));
        ArrayList<SoftwarePackage> softwarePackageList = new ArrayList<>();
        softwarePackageList.addAll(softwarePackageRepository.listAll());
        // sort the software packages in dependency order
        Collections.sort(softwarePackageList, new DependencyComparator<SoftwarePackage>());
        log.debug("software packages: {}", mapper.writeValueAsString(softwarePackageList));
        for(SoftwarePackage softwarePackage : softwarePackageList) {
            log.debug("software package: {}", softwarePackage.getPackageName());
        }
        
    }
    
    @Test
    public void testSoftwarePackageComparison() throws IOException {
        JsonSoftwarePackageRepository softwarePackageRepository = new JsonSoftwarePackageRepository(getClass().getResourceAsStream("/software-packages.json"));
        SoftwarePackage attestationService = softwarePackageRepository.searchByNameEquals("attestation_service");
        SoftwarePackage trustAgent = softwarePackageRepository.searchByNameEquals("trust_agent");
        SoftwarePackage trustDirector = softwarePackageRepository.searchByNameEquals("trust_director");
        SoftwarePackage keyBroker = softwarePackageRepository.searchByNameEquals("key_broker");
        SoftwarePackage keyBrokerProxy = softwarePackageRepository.searchByNameEquals("key_broker_proxy");
        SoftwarePackage openstackExtensions = softwarePackageRepository.searchByNameEquals("openstack_extensions");
        DependencyComparator<SoftwarePackage> comparator = new DependencyComparator<>();
        log.debug("attestationService <> trustAgent  ... {}", comparator.compare(attestationService, trustAgent));         // -1
        log.debug("attestationService <> trustDirector  ... {}", comparator.compare(attestationService, trustDirector));   // -1
        log.debug("attestationService <> keyBroker  ... {}", comparator.compare(attestationService, keyBroker));           // -1
        log.debug("keyBroker <> trustAgent  ... {}", comparator.compare(keyBroker, trustAgent));                           // 0
        log.debug("keyBroker <> trustDirector  ... {}", comparator.compare(keyBroker, trustDirector));                     // -1  .... but when sorted in entire list,  it ends up AFTER trust_director... how??
        log.debug("keyBroker <> keyBrokerProxy  ... {}", comparator.compare(keyBroker, keyBrokerProxy));                   // 0   .... correct, key_broker_proxy doesn't actually need key_broker FOR INSTALLATION;  at runtime the key broker url's come from the trusted hosts in key requests
        log.debug("openstackExtensions <> trustAgent  ... {}", comparator.compare(openstackExtensions, trustAgent));       // 0   .... correct, trustagent doesn't depend on openstack controller for installation, or the other way around
        log.debug("openstackExtensions <> trustDirector  ... {}", comparator.compare(openstackExtensions, trustDirector)); // -1  .... correct,  director needs to know the openstack info prior to install when openstack integration is selected
        log.debug("openstackExtensions <> keyBroker  ... {}", comparator.compare(openstackExtensions, keyBroker));         // 0   .... correct,  no relation at all.
        ArrayList<SoftwarePackage> list1 = new ArrayList<>();
        list1.addAll(Arrays.asList(openstackExtensions, keyBroker, keyBrokerProxy, trustAgent, trustDirector, attestationService));
        Collections.sort(list1, comparator);
        for(SoftwarePackage softwarePackage : list1) {
            log.debug(" 1 software package: {}", softwarePackage.getPackageName());
        }
        ArrayList<SoftwarePackage> list2 = new ArrayList<>();
        list2.addAll(Arrays.asList(keyBrokerProxy, attestationService, keyBroker, openstackExtensions, trustAgent, trustDirector ));
        Collections.sort(list2, comparator);
        for(SoftwarePackage softwarePackage : list2) {
            log.debug(" 2 software package: {}", softwarePackage.getPackageName());
        }
        
    }
    
}
