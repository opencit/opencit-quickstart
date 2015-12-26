/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.wizard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.deployment.Feature;
import com.intel.mtwilson.deployment.FeatureRepository;
import com.intel.mtwilson.deployment.FeatureRepositoryFactory;
import com.intel.mtwilson.deployment.FeatureUtils;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.OrderAware;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.SoftwarePackageRepository;
import com.intel.mtwilson.deployment.SoftwarePackageRepositoryAware;
import com.intel.mtwilson.deployment.SoftwarePackageRepositoryFactory;
import com.intel.mtwilson.deployment.SoftwarePackageUtils;
import com.intel.mtwilson.deployment.TargetAware;
import com.intel.mtwilson.deployment.conditions.FeatureAvailable;
import com.intel.mtwilson.deployment.conditions.FeatureDependenciesIncluded;
import com.intel.mtwilson.deployment.conditions.SoftwarePackageAvailable;
import com.intel.mtwilson.deployment.conditions.SoftwarePackageDependenciesIncluded;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.CreateTrustAgentUserInAttestationService;
import com.intel.mtwilson.deployment.task.CreateTrustDirectorUserInAttestationService;
import com.intel.mtwilson.deployment.task.CreateTrustDirectorUserInKeyBroker;
import com.intel.mtwilson.deployment.task.FileTransfer;
import com.intel.mtwilson.deployment.task.PostconfigureAttestationService;
import com.intel.mtwilson.deployment.task.PostconfigureKeyBroker;
import com.intel.mtwilson.deployment.task.PreconfigureAttestationService;
import com.intel.mtwilson.deployment.task.PreconfigureKeyBroker;
import com.intel.mtwilson.deployment.task.PreconfigureKeyBrokerProxy;
import com.intel.mtwilson.deployment.task.PreconfigureOpenstackExtensions;
import com.intel.mtwilson.deployment.task.PreconfigureTrustAgent;
import com.intel.mtwilson.deployment.task.PreconfigureTrustDirector;
import com.intel.mtwilson.deployment.task.RemoteInstall;
import com.intel.mtwilson.deployment.task.SynchronizeSoftwarePackageTargets;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.task.AbstractTask;
import com.intel.mtwilson.util.task.DependencyComparator;
import com.intel.mtwilson.util.task.Task;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * create dependencies between products installations:
 * <pre>
 * trust_agent depends on attestation_service
 * key_broker_proxy depends on attestation_service
 * key_broker depends on attestation_service
 * trust_director depends on attestation_service and key_broker (if present) and openstack_extensions (if present)
 * openstack_extensions depends on attestation_service
 * attestation_service (does not have dependencies)
 * </pre>
 *
 * @author jbuhacoff
 */
public class DeploymentTaskFactory extends AbstractTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeploymentTaskFactory.class);
    private SoftwarePackageRepository softwarePackageRepository;
    private FeatureRepository featureRepository;
    private OrderDocument order;
    private List<Feature> availableFeatureList;
    private Map<String,Feature> availableFeatureMap;
    private List<Feature> selectedFeatures;
    private List<SoftwarePackage> availableSoftwarePackageList;
    private Map<String,SoftwarePackage> availableSoftwarePackageMap;
    private List<SoftwarePackage> selectedSoftwarePackages;
    private Map<String,SoftwarePackage> selectedSoftwarePackageMap;
    private ArrayList<Task> output;

    public DeploymentTaskFactory(OrderDocument request) throws IOException {
        softwarePackageRepository = SoftwarePackageRepositoryFactory.getInstance();
        featureRepository = FeatureRepositoryFactory.getInstance();

        order = request;

        // setup preconditions to validate input before execution

        // do we support all the specified features?
        availableFeatureList = featureRepository.listAll();
        
//        ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
//        log.debug("Read features: {}", mapper.writeValueAsString(availableFeatureList));
        
        availableFeatureMap = FeatureUtils.mapFeatures(availableFeatureList);
        Set<String> selectedFeatureNames = request.getFeatures();
        precondition(new FeatureAvailable(availableFeatureMap, selectedFeatureNames));

        // do the specified features include all required dependencies?
        selectedFeatures = createFeatureList(selectedFeatureNames);
        precondition(new FeatureDependenciesIncluded(selectedFeatures));

        // do we have all the specified software packages?
        availableSoftwarePackageList = softwarePackageRepository.listAll();
        availableSoftwarePackageMap = SoftwarePackageUtils.mapSoftwarePackages(availableSoftwarePackageList);
        Set<String> selectedSoftwarePackageNames = getSoftwarePackageNamesFromTargets(request.getTargets());
        precondition(new SoftwarePackageAvailable(availableSoftwarePackageMap, selectedSoftwarePackageNames));

        // do the specified software packages include all required dependencies?
        selectedSoftwarePackages = createSoftwarePackageList(selectedSoftwarePackageNames);
        precondition(new SoftwarePackageDependenciesIncluded(selectedSoftwarePackages));
        
        // map the selected software packages (across all targets) so it's easy for tasks to see what else is going on (useful for conditional integration tasks)
        selectedSoftwarePackageMap = SoftwarePackageUtils.mapSoftwarePackages(selectedSoftwarePackages);
    }

    /**
     * <pre>
     * Set<Target> targets = request.getTargets(); // each one with host, port, username, password, publicKeyDigest, timeout, networkRole, and packages list with any of [key_broker, key_broker_proxy, trust_director, trust_agent, attestation_service, openstack_extensions]
     *
     * // validate input:  do we support all the specified features?
     * Set<String> selectedfeatureNames = request.getFeatures();
     * testFeaturesAvailable(request.getFeatures());
     *
     * // validate input:  do the specified features include all required dependencies?
     * List<Feature> selectedFeatures = createFeatureList(selectedfeatureNames);
     * testFeatureDependenciesIncluded(selectedFeatures);
     *
     * // validate input:  do we have all the specified software packages?
     * Set<String> selectedSoftwarePackageNames = getSoftwarePackageNamesFromTargets(targets);
     * testSoftwarePackagesAvailable(selectedSoftwarePackageNames);
     *
     * // validate input:  do the specified software packages include all required dependencies?
     * List<SoftwarePackage> selectedSoftwarePackages = createSoftwarePackageList(selectedSoftwarePackageNames);
     * testSoftwarePackageDependenciesIncluded(selectedSoftwarePackages);
     * </pre>
     */
    
    

    /**
     * Creates the tasks necessary to install the given software package on the
     * given target
     *
     * @param softwarePackage
     * @param target
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private List<Task> createSoftwarePackageTargetTasks(SoftwarePackage softwarePackage, Target target) {
        ArrayList<Task> tasks = new ArrayList<>();

        // copy the installer...  currently this is a generic step for each package.
        File packageFile = softwarePackage.getFile(); // must have already been validated by precondition; if precondition wasn't checked there could be NPE here
        String remoteFilePath = packageFile.getName();
        FileTransfer fileTransfer = new FileTransfer(target, new FileTransferDescriptor(packageFile, remoteFilePath));
        tasks.add(fileTransfer);
        // remote install task ... we initialize it here so that when we make the file transfer & pre-configuration tasks below they can add themselves to this as dependencies
        RemoteInstall remoteInstall = new RemoteInstall(target, softwarePackage);
        remoteInstall.getDependencies().add(fileTransfer);
        tasks.add(remoteInstall);
        // generate the env file... we need a different class for each package but each of them behaves the same by generating the file and then creating a manifest for file transfer
        if( softwarePackage.getPackageName().equals("attestation_service")) {
            PreconfigureAttestationService generateEnvFile = new PreconfigureAttestationService();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile);
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // get the mtwilson tls cert sha1 fingerprint so other services can use it when connecting
            PostconfigureAttestationService postconfigureAttestationService = new PostconfigureAttestationService(target);
            postconfigureAttestationService.getDependencies().add(remoteInstall);
            tasks.add(postconfigureAttestationService);
            // IF the order includes trust director too (any target host), then we need to create a user for director to connect to mtwilson (see bug #4866)
            if( selectedSoftwarePackageMap.containsKey("trust_director") ) {
                CreateTrustDirectorUserInAttestationService createDirectorUser = new CreateTrustDirectorUserInAttestationService(target);
                createDirectorUser.getDependencies().add(postconfigureAttestationService);
                tasks.add(createDirectorUser);
            }
            if( selectedSoftwarePackageMap.containsKey("trust_agent") ) {
                CreateTrustAgentUserInAttestationService createTrustagentUser = new CreateTrustAgentUserInAttestationService(target);
                createTrustagentUser.getDependencies().add(postconfigureAttestationService);
                tasks.add(createTrustagentUser);
            }
        }
        if( softwarePackage.getPackageName().equals("key_broker")) {
            PreconfigureKeyBroker generateEnvFile = new PreconfigureKeyBroker();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile);
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // get the kms tls cert sha1 fingerprint so other services can use it when connecting
            PostconfigureKeyBroker postconfigureKeyBroker = new PostconfigureKeyBroker(target);
            postconfigureKeyBroker.getDependencies().add(remoteInstall);
            tasks.add(postconfigureKeyBroker);
            // IF the order includes trust director too (any target host), then we need to create a user for director to connect to key broker (see bug #4866)
            if( selectedSoftwarePackageMap.containsKey("trust_director") ) {
                CreateTrustDirectorUserInKeyBroker createDirectorUser = new CreateTrustDirectorUserInKeyBroker(target);
                createDirectorUser.getDependencies().add(postconfigureKeyBroker);
                tasks.add(createDirectorUser);
            }
        }
        if( softwarePackage.getPackageName().equals("key_broker_proxy")) {
            PreconfigureKeyBrokerProxy generateEnvFile = new PreconfigureKeyBrokerProxy();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile);
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
        }
        if( softwarePackage.getPackageName().equals("trust_director")) {
            PreconfigureTrustDirector generateEnvFile = new PreconfigureTrustDirector();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile);
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
        }
        if( softwarePackage.getPackageName().equals("trust_agent")) {
            PreconfigureTrustAgent generateEnvFile = new PreconfigureTrustAgent();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile);
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
        }
        if( softwarePackage.getPackageName().equals("openstack_extensions")) {
            PreconfigureOpenstackExtensions generateEnvFile = new PreconfigureOpenstackExtensions();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile);
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
        }
        
        // provide context to tasks according to their needs
        // this means tasks must not try to use this context except before execute()
        // is called (later)
        for(Task task : tasks) {
            if( task instanceof OrderAware ) {
                OrderAware orderAwareTask = (OrderAware)task;
                orderAwareTask.setOrderDocument(order);
            }
            if( task instanceof TargetAware ) {
                TargetAware targetAwareTask = (TargetAware)task;
                targetAwareTask.setTarget(target);
            }
            if( task instanceof SoftwarePackageRepositoryAware ) {
                SoftwarePackageRepositoryAware softwarePackageRepositoryAwareTask = (SoftwarePackageRepositoryAware)task;
                softwarePackageRepositoryAwareTask.setSoftwarePackageRepository(softwarePackageRepository);
            }
        }
        return tasks;
    }

    private Task createSoftwarePackageSyncTask(SoftwarePackage softwarePackage, Collection<Target> targets) {
        SynchronizeSoftwarePackageTargets task = new SynchronizeSoftwarePackageTargets(softwarePackage, targets);
        return task;
    }


    /**
     * Given a collection of targets, each specifying one or more packages to
     * install, determine the set of all packages to install across all targets.
     * This is used for input validation, to ensure that all specified packages
     * are actually available before we start executing.
     *
     * @param targets
     * @return
     */
    private Set<String> getSoftwarePackageNamesFromTargets(Collection<Target> targets) {
        HashSet<String> set = new HashSet<>();
        for (Target target : targets) {
            set.addAll(target.getPackages());
        }
        return set;
    }

    /**
     * Creates a list of names of the available software packages, in dependency
     * order, meaning the first one should be installed first, then the second
     * one, etc. This ordered list can then be used as the basis for sorting a
     * set of tasks
     *
     * @return
     */
    private List<String> createOrderedAvailableSoftwarePackageNameList() {
        List<SoftwarePackage> list = softwarePackageRepository.listAll();
        Collections.sort(list, new DependencyComparator<SoftwarePackage>());
        ArrayList<String> sortedNames = new ArrayList<>();
        for (SoftwarePackage item : list) {
            sortedNames.add(item.getPackageName());
        }
        return Collections.unmodifiableList(sortedNames);
    }

    private List<String> createOrderedAvailableFeatureNameList() {
        List<Feature> list = featureRepository.listAll();
        Collections.sort(list, new DependencyComparator<Feature>());
        ArrayList<String> sortedNames = new ArrayList<>();
        for (Feature item : list) {
            sortedNames.add(item.getName());
        }
        return Collections.unmodifiableList(sortedNames);
    }

    private List<SoftwarePackage> createSoftwarePackageList(Collection<String> softwarePackageNames) throws IOException {
        ArrayList<SoftwarePackage> list = new ArrayList<>();
        for (String name : softwarePackageNames) {
            SoftwarePackage softwarePackage = softwarePackageRepository.searchByNameEquals(name);
            if (softwarePackage == null) {
                log.debug("Software package {} does not exist", name);
            }
            else {
                list.add(softwarePackage);
            }
        }
        return list;
    }


//private ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
    private List<Feature> createFeatureList(Collection<String> featureNames) throws IOException {
        ArrayList<Feature> list = new ArrayList<>();
        for (String name : featureNames) {
            Feature feature = availableFeatureMap.get(name); //featureRepository.getFeatureByName(name);
//            log.error("Feature {}: {}", name, mapper.writeValueAsString(feature));
            if (feature == null) {
                //throw new IllegalArgumentException("Feature does not exist");
                log.debug("Feature does not exist: {}", name);
                continue;
            }
            list.add(feature);
        }
        return list;
    }


    /**
     * Given a deployment request with a set of features, targets, and settings,
     * creates a collection of tasks configured to deploy the software as
     * specified. This collection may be passed to a task manager for execution.
     *
     * Each target in the deployment request specifies a set of packages to
     * install on it. We expand the structure from a small graph
     * target->packages to a list of pairs (target,package1), (target,package2),
     * so that we can use the package dependency order (from our package
     * repository) to create dependencies across these pairs, and then sort them
     * in dependency order so caller gets an ordered list of tasks to execute.
     * 
     */
    @Override
    public void execute() {
        // load server data
//        List<String> orderedAvailableFeatureNameList = createOrderedAvailableFeatureNameList();
//        List<String> orderedAvailableSoftwarePackageNameList = createOrderedAvailableSoftwarePackageNameList();
        
        // each target specifies host, port, username, password, publicKeyDigest, timeout, networkRole, and packages list with any of [key_broker, key_broker_proxy, trust_director, trust_agent, attestation_service, openstack_extensions]
        Set<Target> targets = order.getTargets(); 
        log.debug("targets: {}", targets.size());
        // the software packages are installed in groups:  given the list of software
        // packages to install, each package wil be installed on all the targets that
        // specify it before we move on to the next group.  this enables us to 
        // add steps that synchronize clusters of software packages, for example
        // if the request includes two attestation services for high availability,
        // then these would be installed first, then would be synchronized so each
        // of them  has the other's certs, so that when other software is installed
        // it will have the complete configuration available (both certs).
        Collections.sort(selectedSoftwarePackages, new DependencyComparator<SoftwarePackage>());
//        List<SoftwarePackage> orderedSelectedSoftwarePackages = //createSoftwarePackageList(orderedAvailableSoftwarePackageNameList);
        log.debug("Creating new output list");
        output = new ArrayList<>();
        Task priorSyncTask = null;
        for (SoftwarePackage softwarePackage : selectedSoftwarePackages) {
            log.debug("processing software package: {}", softwarePackage.getPackageName());
            // make a task for each target that specifies this software package
            HashSet<Target> applicableTargets = new HashSet<>();
            ArrayList<Task> softwarePackageTasks = new ArrayList<>();
            for (Target target : targets) {
                if (target.getPackages().contains(softwarePackage.getPackageName())) {
                    log.debug("adding target: {}", target.getHost());
                    softwarePackageTasks.addAll(createSoftwarePackageTargetTasks(softwarePackage, target));
                    applicableTargets.add(target);
                }
            }
            log.debug("making sync task");
            // make a synchronization task for all those targets
            Task softwarePackageSyncTask = createSoftwarePackageSyncTask(softwarePackage, applicableTargets);
            softwarePackageSyncTask.getDependencies().addAll(softwarePackageTasks);

            // set the sync task of the prior software group as a dependency
            // of this sync task, to ensure the prior group installs before
            // this group does.  
            if (priorSyncTask != null) {
                softwarePackageSyncTask.getDependencies().add(priorSyncTask);
            }
            priorSyncTask = softwarePackageSyncTask;

            // now add all the software package tasks and the sync task
            // to the output task list
            output.addAll(softwarePackageTasks);
            output.add(softwarePackageSyncTask);
        }
    }

    /**
     * 
     * @return the list of tasks generated by run(), or null if called before run()
     */
    public List<Task> getOutput() {
        return output;
    }
    
}
