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
import com.intel.mtwilson.util.task.DependenciesUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
//        LoggingDependencyComparator<SoftwarePackage> comparator = new LoggingDependencyComparator<>();
//        Collections.sort(softwarePackageList, comparator);
//        bubblesort(softwarePackageList, comparator);
        Collections.shuffle(softwarePackageList);
        DependenciesUtil.sort(softwarePackageList);

//        log.debug("software packages: {}", mapper.writeValueAsString(softwarePackageList));
        for (SoftwarePackage softwarePackage : softwarePackageList) {
            log.debug("software package: {} ({})", softwarePackage.getPackageName(), softwarePackage);
        }
        /*
         Collections.shuffle(softwarePackageList);
         for(int i=0; i<softwarePackageList.size()-1; i++) {
         log.debug("software package: {} vs. {}   ... {}", softwarePackageList.get(i).getPackageName(), softwarePackageList.get(i+1).getPackageName(), comparator.compare(softwarePackageList.get(i), softwarePackageList.get(i+1)));
         }
         */
    }

    @Test
    public void testSoftwarePackageComparison() throws IOException {
        JsonSoftwarePackageRepository softwarePackageRepository = new JsonSoftwarePackageRepository(getClass().getResourceAsStream("/software-packages.json"));
        SoftwarePackage attestationService = softwarePackageRepository.searchByNameEquals("attestation_service");
        SoftwarePackage trustAgent = softwarePackageRepository.searchByNameEquals("trustagent");
        SoftwarePackage trustDirector = softwarePackageRepository.searchByNameEquals("director");
        SoftwarePackage keyBroker = softwarePackageRepository.searchByNameEquals("key_broker");
        SoftwarePackage keyBrokerProxy = softwarePackageRepository.searchByNameEquals("key_broker_proxy");
        SoftwarePackage openstackExtensions = softwarePackageRepository.searchByNameEquals("openstack_extensions");
        ArrayList<SoftwarePackage> list1 = new ArrayList<>();
        list1.addAll(Arrays.asList(openstackExtensions, keyBroker, keyBrokerProxy, trustAgent, trustDirector, attestationService));
        DependenciesUtil.sort(list1);
        for (SoftwarePackage softwarePackage : list1) {
            log.debug(" 1 software package: {}", softwarePackage.getPackageName());
        }
        ArrayList<SoftwarePackage> list2 = new ArrayList<>();
        list2.addAll(Arrays.asList(keyBrokerProxy, attestationService, keyBroker, openstackExtensions, trustAgent, trustDirector));
        DependenciesUtil.sort(list2);
        for (SoftwarePackage softwarePackage : list2) {
            log.debug(" 2 software package: {}", softwarePackage.getPackageName());
        }

    }

    public static class LoggingDependencyComparator<T extends Dependencies> implements Comparator<Dependencies<T>> {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingDependencyComparator.class);

        /**
         *
         * @param task1
         * @param task2
         * @return -1 if task1 should run before task2, 0 if they can run in any
         * order, 1 if task1 should run after task2
         */
        @Override
        public int compare(Dependencies<T> task1, Dependencies<T> task2) {
            log.debug("a[{}] <> b[{}] a->b? {}  b->a? {}", task1, task2, isDependent(task1, task2), isDependent(task2, task1));
            if (task1 == null || task2 == null) {
                throw new NullPointerException();
            }
            if (isDependent(task1, task2)) {
                return 1;
            }
            if (isDependent(task2, task1)) {
                return -1;
            }
            return 0;
        }

        /**
         *
         * @param subject must not be null
         * @param other can be null; isDependent(x,null) is false for any x
         * @return true if subject is dependent on other, or in other words, if
         * other appears in the dependency graph of subject
         */
        private boolean isDependent(Dependencies<T> subject, Dependencies<T> other) {
            if (subject == null) {
                throw new NullPointerException();
            }
            if (other == null) {
                return false;
            }
            Collection<T> dependencies = subject.getDependencies();
            if (dependencies == null) {
                return false;
            }
            for (T t : dependencies) {
                if (t == null) {
                    continue;
                }
                if (t.equals(other)) {
                    // subject directly dependent on other
                    return true;
                }
                if (isDependent(t, other)) {
                    // subject indirectly dependent on other, through t
                    return true;
                }
            }
            // subject is not directly or transitively dependent on other task
            return false;
        }
    }

    public static <T> void bubblesort(List<T> list, Comparator<? super T> comparator) {
        boolean swapped = true;
        int j = 0;
//        T tmp;
        int length = list.size();
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < length - j; i++) {
                T a, b;
                a = list.get(i);
                b = list.get(i + 1);
                if (comparator.compare(a, b) == 1) {
//                    tmp = a;
                    list.set(i, b);
                    list.set(i + 1, a);
                    swapped = true;
                }
            }
        }
    }


}
