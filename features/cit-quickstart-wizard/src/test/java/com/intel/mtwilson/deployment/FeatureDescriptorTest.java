/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.descriptor.FeatureDescriptorCollection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.task.DependenciesUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class FeatureDescriptorTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeatureDescriptorTest.class);
    private static final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();

    @Test
    public void testReadFeaturesJson() throws IOException {
        FeatureDescriptorCollection descriptorCollection = mapper.readValue(getClass().getResourceAsStream("/features.json"), FeatureDescriptorCollection.class);
        log.debug("features: {}", mapper.writeValueAsString(descriptorCollection));
    }
    
    @Test
    public void testFeatureFactory() throws IOException {
        JsonSoftwarePackageRepository softwarePackageRepository = new JsonSoftwarePackageRepository(getClass().getResourceAsStream("/software.json"));
        JsonFeatureRepository featureRepository = new JsonFeatureRepository(getClass().getResourceAsStream("/features.json"), softwarePackageRepository);
        List<Feature> featureList = featureRepository.listAll();
        DependenciesUtil.sort(featureList);
        log.debug("features: {}", mapper.writeValueAsString(featureList));
        
    }
}
