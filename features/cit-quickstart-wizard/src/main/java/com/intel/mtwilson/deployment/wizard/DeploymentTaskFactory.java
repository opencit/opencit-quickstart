/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.wizard;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.Feature;
import com.intel.mtwilson.deployment.FeatureRepository;
import com.intel.mtwilson.deployment.FeatureRepositoryFactory;
import com.intel.mtwilson.deployment.FeatureUtils;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.OrderAware;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.SoftwarePackageRepository;
import com.intel.mtwilson.deployment.SoftwarePackageRepositoryAware;
import com.intel.mtwilson.deployment.SoftwarePackageRepositoryFactory;
import com.intel.mtwilson.deployment.SoftwarePackageUtils;
import com.intel.mtwilson.deployment.TargetAware;
import com.intel.mtwilson.deployment.conditions.EnvironmentAvailable;
import com.intel.mtwilson.deployment.conditions.FeatureAvailable;
import com.intel.mtwilson.deployment.conditions.FeatureDependenciesIncluded;
import com.intel.mtwilson.deployment.conditions.FeatureRequiredSoftwarePackagesIncluded;
import com.intel.mtwilson.deployment.conditions.SoftwarePackageAvailable;
import com.intel.mtwilson.deployment.descriptor.NetworkRole;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.faults.Null;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.ApproveKeyBrokerProxyUserInAttestationService;
import com.intel.mtwilson.deployment.task.CopyQuickstartOrder;
import com.intel.mtwilson.deployment.task.CreateAttestationHubUserInAttestationService;
import com.intel.mtwilson.deployment.task.CreateTrustAgentUserInAttestationService;
import com.intel.mtwilson.deployment.task.CreateTrustDirectorUserInAttestationService;
import com.intel.mtwilson.deployment.task.CreateTrustDirectorUserInKeyBroker;
import com.intel.mtwilson.deployment.task.CreateTrustDirectorUserInOpenstack;
import com.intel.mtwilson.deployment.task.DynamicFileTransfer;
import com.intel.mtwilson.deployment.task.FileTransfer;
import com.intel.mtwilson.deployment.task.ImportAttestationServiceCertificatesToKeyBroker;
import com.intel.mtwilson.deployment.task.ImportAttestationServiceCertificatesToOpenstack;
import com.intel.mtwilson.deployment.task.PostconfigureAttestationHub;
import com.intel.mtwilson.deployment.task.PostconfigureAttestationService;
import com.intel.mtwilson.deployment.task.PostconfigureKeyBroker;
import com.intel.mtwilson.deployment.task.PostconfigureOpenstack;
import com.intel.mtwilson.deployment.task.PostconfigureTrustDirector;
import com.intel.mtwilson.deployment.task.PreconfigureAttestationHub;
import com.intel.mtwilson.deployment.task.PreconfigureAttestationService;
import com.intel.mtwilson.deployment.task.PreconfigureKeyBroker;
import com.intel.mtwilson.deployment.task.PreconfigureKeyBrokerProxy;
import com.intel.mtwilson.deployment.task.PreconfigureOpenstackExtensions;
import com.intel.mtwilson.deployment.task.PreconfigureTrustAgent;
import com.intel.mtwilson.deployment.task.PreconfigureTrustDirector;
import com.intel.mtwilson.deployment.task.RemoteInstall;
import com.intel.mtwilson.deployment.task.RetrieveLinuxOperatingSystemVersion;
import com.intel.mtwilson.deployment.task.SynchronizeSoftwarePackageTargets;
import com.intel.mtwilson.deployment.task.TrustAgentFileTransferManifestProvider;
import com.intel.mtwilson.util.task.AbstractTask;
import com.intel.mtwilson.util.task.DependenciesUtil;
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
 * trustagent_ubuntu depends on attestation_service
 * key_broker_proxy depends on attestation_service
 * key_broker depends on attestation_service
 * director depends on attestation_service and key_broker (if present) and openstack_extensions (if present)
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
        order = request;
        softwarePackageRepository = SoftwarePackageRepositoryFactory.getInstance();

        // precondition:  we have a configured feature set for the selected environment  (PRIVATE, PROVIDER, or SUBSCRIBER)
        NetworkRole networkRole = order.getNetworkRole();
        if( networkRole == null ) {
            fault(new Null("network_role"));
            throw new IllegalArgumentException();
        }
        precondition(new EnvironmentAvailable(networkRole.name()));
        featureRepository = FeatureRepositoryFactory.getInstance(networkRole.name());

        // setup preconditions to validate input before execution

        // do we support all the specified features?
        availableFeatureList = featureRepository.listAll();
        
//        ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
//        log.debug("Read features: {}", mapper.writeValueAsString(availableFeatureList));
        
        availableFeatureMap = FeatureUtils.mapFeatures(availableFeatureList);
        Set<String> selectedFeatureNames = request.getFeatures();
        precondition(new FeatureAvailable(availableFeatureMap, selectedFeatureNames));

        // do the specified features include all required feature-dependencies?
        selectedFeatures = createFeatureList(selectedFeatureNames);
        precondition(new FeatureDependenciesIncluded(selectedFeatures));

        // do we have all the specified software packages?
        availableSoftwarePackageList = softwarePackageRepository.listAll();
        availableSoftwarePackageMap = SoftwarePackageUtils.mapSoftwarePackages(availableSoftwarePackageList);
        Set<String> selectedSoftwarePackageNames = getSoftwarePackageNamesFromTargets(request.getTargets());
        precondition(new SoftwarePackageAvailable(availableSoftwarePackageMap, selectedSoftwarePackageNames));

        // do the specified software packages include all required software for selected features?
        // NOTE: feature dependencies are checked by another precondition so we don't need to 
        // check transitively.
        selectedSoftwarePackages = createSoftwarePackageList(selectedSoftwarePackageNames);
        precondition(new FeatureRequiredSoftwarePackagesIncluded(selectedFeatures, selectedSoftwarePackages));
        
        // we don't check this one because the dependencies also include software that mayh not have been
        // selected to install...  so would give a "false negative" by failing in a case where
        // trust director is selected but key broker isn't because vm encryption was not chosen.
//        precondition(new SoftwarePackageDependenciesIncluded(selectedSoftwarePackages));
        
        // map the selected software packages (across all targets) so it's easy for tasks to see what else is going on (useful for conditional integration tasks)
        selectedSoftwarePackageMap = SoftwarePackageUtils.mapSoftwarePackages(selectedSoftwarePackages);
    }

    /**
     * <pre>
     * Set<Target> targets = request.getTargets(); // each one with host, port, username, password, publicKeyDigest, timeout, networkRole, and packages list with any of [key_broker, key_broker_proxy, director, trustagent_ubuntu, attestation_service, openstack_extensions]
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

    private Task createMonitorScriptFileTransferTask(Target target) {
        // copy the installer, marker file, and monitor script...  currently this is a generic step for each package.
        ArrayList<FileTransferDescriptor> packageList = new ArrayList<>();
        File packageInstallerMonitorScriptFile = new File(Folders.repository("scripts") + File.separator + "monitor.sh");
        packageList.add(new FileTransferDescriptor(packageInstallerMonitorScriptFile, packageInstallerMonitorScriptFile.getName()));
        FileTransfer fileTransfer = new FileTransfer(target, packageList);
        return fileTransfer;
    }
    
    
    private Task createStaticFileTransferTask(SoftwarePackage softwarePackage, Target target) {
        // copy the installer, marker file, and monitor script...  currently this is a generic step for each package.
        ArrayList<FileTransferDescriptor> packageList = new ArrayList<>();
        List<File> packageFiles = softwarePackage.getFiles("default"); // must have already been validated by precondition; if precondition wasn't checked there could be NPE here
        if (packageFiles != null && !packageFiles.isEmpty()) {
            for(File packageFile: packageFiles){
                log.debug("Adding file {} to package {}",packageFile.getName(), softwarePackage.getPackageName());
                packageList.add(new FileTransferDescriptor(packageFile, packageFile.getName()));
            }
        }
        FileTransfer fileTransfer = new FileTransfer(target, packageList);
        return fileTransfer;
    }
    
    private Task createDynamicFileTransferTask(SoftwarePackage softwarePackage, Target target, RetrieveLinuxOperatingSystemVersion retrieveLinuxOperatingSystemVersion) {
        DynamicFileTransfer fileTransfer = new DynamicFileTransfer(target, new TrustAgentFileTransferManifestProvider(softwarePackage, retrieveLinuxOperatingSystemVersion));
        return fileTransfer;        
    }
    
    
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

        Task fileTransferMonitorScript = createMonitorScriptFileTransferTask(target);
        tasks.add(fileTransferMonitorScript);
        
        // remote install task ... we initialize it here so that when we make the file transfer & pre-configuration tasks below they can add themselves to this as dependencies
        RemoteInstall remoteInstall = new RemoteInstall(target, softwarePackage); // assumes /bin/bash,  a .mark file, and a monitor.sh file
        remoteInstall.getDependencies().add(fileTransferMonitorScript);
        tasks.add(remoteInstall);
        // generate the env file... we need a different class for each package but each of them behaves the same by generating the file and then creating a manifest for file transfer
        if( softwarePackage.getPackageName().equals("attestation_service")) {
            Task staticFileTransfer = createStaticFileTransferTask(softwarePackage, target);
            tasks.add(staticFileTransfer);
            remoteInstall.getDependencies().add(staticFileTransfer);
            
            PreconfigureAttestationService generateEnvFile = new PreconfigureAttestationService();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // get the mtwilson tls cert sha256 fingerprint so other services can use it when connecting
            PostconfigureAttestationService postconfigureAttestationService = new PostconfigureAttestationService(target);
            postconfigureAttestationService.getDependencies().add(remoteInstall);
            tasks.add(postconfigureAttestationService);
            // copy the order file to attestation service
            CopyQuickstartOrder copyQuickstartOrder = new CopyQuickstartOrder(softwarePackage);
            copyQuickstartOrder.getDependencies().add(remoteInstall);
            tasks.add(copyQuickstartOrder);
            
            // IF the order includes trust director too (any target host), then we need to create a user for director to connect to mtwilson (see bug #4866)
            if( selectedSoftwarePackageMap.containsKey("director") ) {
                CreateTrustDirectorUserInAttestationService createDirectorUser = new CreateTrustDirectorUserInAttestationService(target);
                createDirectorUser.getDependencies().add(postconfigureAttestationService);
                tasks.add(createDirectorUser);
            }
            if( selectedSoftwarePackageMap.containsKey("attestation_hub") ) {
                CreateAttestationHubUserInAttestationService createAttestationHubUserInAttestationService = new CreateAttestationHubUserInAttestationService(target);
                createAttestationHubUserInAttestationService.getDependencies().add(postconfigureAttestationService);
                tasks.add(createAttestationHubUserInAttestationService);
            }
            if( selectedSoftwarePackageMap.containsKey("trustagent_ubuntu") ) {
                CreateTrustAgentUserInAttestationService createTrustagentUser = new CreateTrustAgentUserInAttestationService(target);
                createTrustagentUser.getDependencies().add(postconfigureAttestationService);
                tasks.add(createTrustagentUser);
            }
        }
        String packageName = softwarePackage.getPackageName();
        if( softwarePackage.getPackageName().equals("key_broker")) {
            Task staticFileTransfer = createStaticFileTransferTask(softwarePackage, target);
            tasks.add(staticFileTransfer);
            remoteInstall.getDependencies().add(staticFileTransfer);
            
            PreconfigureKeyBroker generateEnvFile = new PreconfigureKeyBroker();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // get the kms tls cert sha256 fingerprint so other services can use it when connecting
            PostconfigureKeyBroker postconfigureKeyBroker = new PostconfigureKeyBroker(target);
            postconfigureKeyBroker.getDependencies().add(remoteInstall);
            tasks.add(postconfigureKeyBroker);
            // IF the order includes attestation service, get the data bundle... otherwise... TODO: need user to upload it in UI
            if( selectedSoftwarePackageMap.containsKey("attestation_service")) {
                ImportAttestationServiceCertificatesToKeyBroker importDataBundle = new ImportAttestationServiceCertificatesToKeyBroker();
                importDataBundle.getDependencies().add(postconfigureKeyBroker);
                tasks.add(importDataBundle);
            }
            // IF the order includes trust director too (any target host), then we need to create a user for director to connect to key broker (see bug #4866)
            if( selectedSoftwarePackageMap.containsKey("director") ) {
                CreateTrustDirectorUserInKeyBroker createDirectorUser = new CreateTrustDirectorUserInKeyBroker(target);
                createDirectorUser.getDependencies().add(postconfigureKeyBroker);
                tasks.add(createDirectorUser);
            }
        }
        if( softwarePackage.getPackageName().equals("key_broker_proxy")) {
            Task staticFileTransfer = createStaticFileTransferTask(softwarePackage, target);
            tasks.add(staticFileTransfer);
            remoteInstall.getDependencies().add(staticFileTransfer);
            
            PreconfigureKeyBrokerProxy generateEnvFile = new PreconfigureKeyBrokerProxy();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // IF the order includes attestation service, then approve the key broker proxy user automatically
            if( selectedSoftwarePackageMap.containsKey("attestation_service")) {
                // note: the "target" here is the key broker proxy host, to 
                // which this task will connect to retrieve the key broker proxy
                // username from its configuration.
                ApproveKeyBrokerProxyUserInAttestationService approveKeyBrokerUser = new ApproveKeyBrokerProxyUserInAttestationService(target);
                approveKeyBrokerUser.getDependencies().add(remoteInstall);
                tasks.add(approveKeyBrokerUser);
            }
        }
        if( packageName.equals("attestation_hub")) {
            Task staticFileTransfer = createStaticFileTransferTask(softwarePackage, target);
            tasks.add(staticFileTransfer);
            remoteInstall.getDependencies().add(staticFileTransfer);
            
            PreconfigureAttestationHub generateEnvFile = new PreconfigureAttestationHub();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // create an admin user in trust director
            PostconfigureAttestationHub postconfigureAttestationHub = new PostconfigureAttestationHub(target);
            postconfigureAttestationHub.getDependencies().add(remoteInstall);
            tasks.add(postconfigureAttestationHub);
            // copy the order file to trust director
            CopyQuickstartOrder copyQuickstartOrder = new CopyQuickstartOrder(softwarePackage);
            copyQuickstartOrder.getDependencies().add(remoteInstall);
            tasks.add(copyQuickstartOrder);
        }
        if( softwarePackage.getPackageName().equals("director")) {
            Task staticFileTransfer = createStaticFileTransferTask(softwarePackage, target);
            tasks.add(staticFileTransfer);
            remoteInstall.getDependencies().add(staticFileTransfer);
            
            PreconfigureTrustDirector generateEnvFile = new PreconfigureTrustDirector();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // create an admin user in trust director
            PostconfigureTrustDirector postconfigureTrustDirector = new PostconfigureTrustDirector(target);
            postconfigureTrustDirector.getDependencies().add(remoteInstall);
            tasks.add(postconfigureTrustDirector);
            // copy the order file to trust director
            CopyQuickstartOrder copyQuickstartOrder = new CopyQuickstartOrder(softwarePackage);
            copyQuickstartOrder.getDependencies().add(remoteInstall);
            tasks.add(copyQuickstartOrder);
        }
        if( softwarePackage.getPackageName().equals("openstack_extensions")) {
            Task staticFileTransfer = createStaticFileTransferTask(softwarePackage, target);
            tasks.add(staticFileTransfer);
            remoteInstall.getDependencies().add(staticFileTransfer);
            
            PreconfigureOpenstackExtensions generateEnvFile = new PreconfigureOpenstackExtensions();
            tasks.add(generateEnvFile);
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            remoteInstall.getDependencies().add(fileTransferEnvFile);
            // create an admin user in openstack
            PostconfigureOpenstack postconfigureOpenstackExtensions = new PostconfigureOpenstack(target);
            postconfigureOpenstackExtensions.getDependencies().add(remoteInstall);
            tasks.add(postconfigureOpenstackExtensions);
            // create a director user in openstack
            if( selectedSoftwarePackageMap.containsKey("director") ) {
                CreateTrustDirectorUserInOpenstack createDirectorUserInOpenstack = new CreateTrustDirectorUserInOpenstack(target);
                createDirectorUserInOpenstack.getDependencies().add(postconfigureOpenstackExtensions);
                tasks.add(createDirectorUserInOpenstack);
            }
            // import the attestation service certificate
            if( selectedSoftwarePackageMap.containsKey("attestation_service") ) {
                ImportAttestationServiceCertificatesToOpenstack importAttestationServiceCertificatesToOpenstack = new ImportAttestationServiceCertificatesToOpenstack();
                importAttestationServiceCertificatesToOpenstack.getDependencies().add(postconfigureOpenstackExtensions);
                tasks.add(importAttestationServiceCertificatesToOpenstack);
            }
        }
        if( softwarePackage.getPackageName().equals("trustagent_ubuntu")) {
            // for trust agent, we need to detect the distribution so we can send appropriate additional files
            RetrieveLinuxOperatingSystemVersion retrieveLinuxOperatingSystemVersion = new RetrieveLinuxOperatingSystemVersion();
            tasks.add(retrieveLinuxOperatingSystemVersion);
            
            PreconfigureTrustAgent generateEnvFile = new PreconfigureTrustAgent(retrieveLinuxOperatingSystemVersion);
            tasks.add(generateEnvFile);
            generateEnvFile.getDependencies().add(retrieveLinuxOperatingSystemVersion);
            
            // copy the env file
            FileTransfer fileTransferEnvFile = new FileTransfer(target, generateEnvFile.getFileTransferManifest());
            fileTransferEnvFile.getDependencies().add(generateEnvFile);
            tasks.add(fileTransferEnvFile);
            
            // currently we don't install trust agent, we just copy the installer and .env files to the host
            // when we will support TA installation we need to update RemoteInstall class to fetch 'executablePath' dynamically based on OperatingSystemVersion
            tasks.remove(remoteInstall);  // the remoteInstall task is defined above for all cases, so we remoe it here to not actually run the installer for trust agent... for now. 
            tasks.remove(fileTransferMonitorScript);//As we do not run remoteInstall task we do not need monitor script also
            
            
            Task dynamicFileTransfer = createDynamicFileTransferTask(softwarePackage, target, retrieveLinuxOperatingSystemVersion);
            tasks.add(dynamicFileTransfer);
            remoteInstall.getDependencies().add(dynamicFileTransfer);
            
            // for trust agent, we have an extra file to send (the tpm patched tools) for both ubuntu and redhat distributions... 
            // but instead of special logic here, it's better to just mention it in config file for both...
            // one possibility is to have a map of operating system name -> additional files and make it generically available to all software packages
            // another possibility is to make software package "variations" with one per supported OS, while keeping software package name global,
            // so we could add a step to detect if the software package is available for the target OS and if not then use the generic package
            // doing that means we have to take all additional files and incldue them in the isntaller itself for that target OS;  
            // so for redhat any extra .rpms that can't be found in rhel repo need to be included in the trustagent_redhat installer.            
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
        /**
         * this needs to be more like this:
         * <pre>
         *         if( softwarePackage.getPackageName().equals("attestation_service")) {
         *             SynchronizeAttestationService task = new SynchronizeAttestationService(softwarePackage, targets);
         *             return task;
         *         }
         *         if( softwarePackage.getPackageName().equals("director")) {
         *             ...
         *         }
         * </pre>
         */
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
    // Method not being used.
    /*private List<String> createOrderedAvailableSoftwarePackageNameList() {
        List<SoftwarePackage> list = softwarePackageRepository.listAll();
        DependenciesUtil.sort(list);
        ArrayList<String> sortedNames = new ArrayList<>();
        for (SoftwarePackage item : list) {
            sortedNames.add(item.getPackageName());
        }
        return Collections.unmodifiableList(sortedNames);
    }*/

    // Method not being used.
    /*private List<String> createOrderedAvailableFeatureNameList() {
        List<Feature> list = featureRepository.listAll();
        DependenciesUtil.sort(list);
        ArrayList<String> sortedNames = new ArrayList<>();
        for (Feature item : list) {
            sortedNames.add(item.getName());
        }
        return Collections.unmodifiableList(sortedNames);
    }*/

    private List<SoftwarePackage> createSoftwarePackageList(Collection<String> softwarePackageNames) throws IOException {
        ArrayList<SoftwarePackage> list = new ArrayList<>();
        for (String name : softwarePackageNames) {
            SoftwarePackage softwarePackage = availableSoftwarePackageMap.get(name); //softwarePackageRepository.searchByNameEquals(name);
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
        
        // each target specifies host, port, username, password, publicKeyDigest, timeout, networkRole, and packages list with any of [key_broker, key_broker_proxy, director, trustagent_ubuntu, attestation_service, openstack_extensions]
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
        DependenciesUtil.sort(selectedSoftwarePackages);
        /*
        if( log.isDebugEnabled()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                log.debug("selected software packages JSON: {}", mapper.writeValueAsString(selectedSoftwarePackages));
                for(SoftwarePackage s : selectedSoftwarePackages) {
                    log.debug("selected software packages name:{} ref:{}", s.getPackageName(), s);
                }
            }
            catch(Exception e) {
                log.error("cannot serialize selected software packages", e);
            }
        }
        */
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
