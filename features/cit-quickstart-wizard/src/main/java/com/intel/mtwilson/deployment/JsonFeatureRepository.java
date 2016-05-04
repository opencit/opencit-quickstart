/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.deployment.descriptor.FeatureDescriptor;
import com.intel.mtwilson.deployment.descriptor.FeatureDescriptorCollection;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class JsonFeatureRepository implements FeatureRepository {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonFeatureRepository.class);
    private static final HashMap<String, Feature> featuresMap = new HashMap<>();
    private final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
    private final SoftwarePackageRepository softwarePackageRepository;

    public JsonFeatureRepository(InputStream in, SoftwarePackageRepository softwarePackageRepository) throws IOException {
        this.softwarePackageRepository = softwarePackageRepository;
        List<Feature> list = read(in);
        for (Feature item : list) {
            featuresMap.put(item.getName(), item);
        }
    }

    public JsonFeatureRepository(FeatureDescriptorCollection descriptorCollection, SoftwarePackageRepository softwarePackageRepository) throws IOException {
        this.softwarePackageRepository = softwarePackageRepository;
        List<Feature> list = read(descriptorCollection);
        for (Feature item : list) {
            featuresMap.put(item.getName(), item);
        }
    }

    private List<Feature> read(FeatureDescriptorCollection descriptorCollection) {
        assert descriptorCollection.getFeatures() != null;
        ArrayList<Feature> featureList = new ArrayList<>();

        // first convert each descriptor to a feature instance with feature name
        for (FeatureDescriptor descriptor : descriptorCollection.getFeatures()) {
            Feature feature = new Feature(descriptor.getName());
            featureList.add(feature);
        }
        // second, make a map of feature name => feature instance
        HashMap<String, Feature> featureMap = new HashMap<>();
        for (Feature feature : featureList) {
            featureMap.put(feature.getName(), feature);
        }
        // third, for each feature, find the dependencies in the map and associate them
        for (FeatureDescriptor descriptor : descriptorCollection.getFeatures()) {
            Feature subject = featureMap.get(descriptor.getName());
            log.debug("Processing feature: {}", subject.getName());
            Collection<String> dependencyNames = descriptor.getDependencies();
            if (dependencyNames != null) {
                for (String dependencyName : dependencyNames) {
                    Feature dependency = featureMap.get(dependencyName);
                    if (dependency != null) {
                        subject.getDependencies().add(dependency);
                    } else {
                        log.warn("Cannot resolve dependency: {}", dependencyName);
                    }
                }
            }
            Collection<String> settingNames = descriptor.getRequiredSettings();
            if( settingNames != null ) {
                subject.getRequiredSettings().addAll(settingNames);
            }
        }
        // fourth, load the software packages and associate each feature instance with the packages specified in its descriptor
        ArrayList<SoftwarePackage> softwarePackageList = new ArrayList<>();
        softwarePackageList.addAll(softwarePackageRepository.listAll());
        HashMap<String, SoftwarePackage> softwarePackageMap = new HashMap<>();
        for (SoftwarePackage softwarePackage : softwarePackageList) {
            softwarePackageMap.put(softwarePackage.getPackageName(), softwarePackage);
        }
        for (FeatureDescriptor featureDescriptor : descriptorCollection.getFeatures()) {
            Feature feature = featureMap.get(featureDescriptor.getName());
            for (String softwarePackageName : featureDescriptor.getSoftwarePackages()) {
                SoftwarePackage softwarePackage = softwarePackageMap.get(softwarePackageName);
                feature.getSoftwarePackages().add(softwarePackage);
            }
        }

        return featureList;
    }

    private List<Feature> read(InputStream in) throws IOException {
        FeatureDescriptorCollection descriptorCollection = mapper.readValue(in, FeatureDescriptorCollection.class);
        return read(descriptorCollection);

    }

    @Override
    public Feature getFeatureByName(String name) {
        return featuresMap.get(name);
    }

    @Override
    public List<Feature> listAll() {
        ArrayList<Feature> list = new ArrayList<>();
        list.addAll(featuresMap.values());
        return list;
    }
}
