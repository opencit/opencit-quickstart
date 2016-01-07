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
import java.util.Collection;
import java.util.HashSet;
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
    
    private SoftwarePackageRepository createSoftwarePackageRepository() throws IOException {
                JsonSoftwarePackageRepository softwarePackageRepository = new JsonSoftwarePackageRepository(getClass().getResourceAsStream("/software.json"));
return softwarePackageRepository;
    }
 
    private FeatureRepository createFeatureRepository(FeatureDescriptorCollection descriptorCollection) throws IOException {
        SoftwarePackageRepository softwarePackageRepository = createSoftwarePackageRepository();
                FeatureRepository instance = new JsonFeatureRepository(descriptorCollection, softwarePackageRepository);
        return instance;
    }
    
    private Collection<String> getRequiredSoftwarePackges(Collection<Feature> features) {
        HashSet<String> requiredSoftwarePackages = new HashSet<>();
        for(Feature feature : features) {
           Collection<SoftwarePackage> softwarePackages = feature.getSoftwarePackages();
           List<String> softwarePackageNames = SoftwarePackageUtils.listSoftwarePackageNames(softwarePackages);
           requiredSoftwarePackages.addAll(softwarePackageNames);
        }
        return requiredSoftwarePackages;
    }
    private Collection<String> getRequiredSettings(Collection<Feature> features) {
        HashSet<String> requiredSettings = new HashSet<>();
        for(Feature feature : features) {
           Collection<String> settingNames = feature.getRequiredSettings();
           requiredSettings.addAll(settingNames);
        }
        return requiredSettings;
    }
    
    /**
     * Expected:
     * feature names: [encryption_vm, attestation_vm, integration_openstack_barbican, integration_openstack_nova, integration_kmip, attestation_host, attestation_host_xm, integration_openstack_glance]
     * required software packages: [trust_director, key_broker_proxy, attestation_service, openstack_extensions, key_broker]
     * required settings: [] 
     * @throws IOException 
     */
    @Test
    public void testFeatureFactoryForPRIVATE() throws IOException {
        FeatureDescriptorCollection descriptorCollection = mapper.readValue(getClass().getResourceAsStream("/PRIVATE.json"), FeatureDescriptorCollection.class);
        log.debug("features: {}", mapper.writeValueAsString(descriptorCollection));
        
        FeatureRepository featureRepository = createFeatureRepository(descriptorCollection);
         
        List<Feature> features = featureRepository.listAll();
        log.debug("feature names: {}", FeatureUtils.listFeatureNames(features));
Collection<String> requiredSoftwarePackages = getRequiredSoftwarePackges(features);
        log.debug("required software packages: {}", requiredSoftwarePackages);
        
        Collection<String> requiredSettings = getRequiredSettings(features);
        log.debug("required settings: {}", requiredSettings);
        
    }
    
    /**
     * 
     * Expected:
     * feature names: [encryption_vm, attestation_vm, integration_openstack_nova, attestation_host, attestation_host_xm]
     * required software packages: [trust_director, key_broker_proxy, attestation_service, openstack_extensions]
     * required settings: [kms.key.provider] 
     * 
     * @throws IOException 
     */
   @Test
    public void testFeatureFactoryForPROVIDER() throws IOException {
        FeatureDescriptorCollection descriptorCollection = mapper.readValue(getClass().getResourceAsStream("/PROVIDER.json"), FeatureDescriptorCollection.class);
                log.debug("features: {}", mapper.writeValueAsString(descriptorCollection));
        FeatureRepository featureRepository = createFeatureRepository(descriptorCollection);
        List<Feature> features = featureRepository.listAll();
        log.debug("feature names: {}", FeatureUtils.listFeatureNames(features));
Collection<String> requiredSoftwarePackages = getRequiredSoftwarePackges(features);
        log.debug("required software packages: {}", requiredSoftwarePackages);
        Collection<String> requiredSettings = getRequiredSettings(features);
        log.debug("required settings: {}", requiredSettings);

    }
   
    /**
     * Expected:
     * feature names: [encryption_vm, attestation_vm, integration_openstack_barbican, integration_kmip, integration_openstack_glance]
     * required software packages: [trust_director, key_broker]
     * required settings: [director.mtwilson.password, 
     * director.mtwilson.username, kms.barbican.project, 
     * director.glance.username, kms.barbican.url, mtwilson.host, 
     * mtwilson.tls.cert.sha1, director.glance.port, director.glance.host, 
     * kms.key.provider, director.glance.tenant, mtwilson.port.https, 
     * director.glance.password]
     * @throws IOException 
     */
   @Test
    public void testFeatureFactoryForSUBSCRIBER() throws IOException {
        FeatureDescriptorCollection descriptorCollection = mapper.readValue(getClass().getResourceAsStream("/SUBSCRIBER.json"), FeatureDescriptorCollection.class);
                log.debug("features: {}", mapper.writeValueAsString(descriptorCollection));
        FeatureRepository featureRepository = createFeatureRepository(descriptorCollection);
        List<Feature> features = featureRepository.listAll();
        log.debug("feature names: {}", FeatureUtils.listFeatureNames(features));
Collection<String> requiredSoftwarePackages = getRequiredSoftwarePackges(features);
        log.debug("required software packages: {}", requiredSoftwarePackages);
        Collection<String> requiredSettings = getRequiredSettings(features);
        log.debug("required settings: {}", requiredSettings);

    }   
}
