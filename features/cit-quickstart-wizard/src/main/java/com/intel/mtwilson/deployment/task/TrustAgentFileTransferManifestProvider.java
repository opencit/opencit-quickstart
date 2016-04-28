/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.wizard.DeploymentTaskFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author boskisha
 */
public class TrustAgentFileTransferManifestProvider implements FileTransferManifestProvider {
        private RetrieveLinuxOperatingSystemVersion retrieveLinuxOperatingSystemVersion;
        private SoftwarePackage softwarePackage;
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TrustAgentFileTransferManifestProvider.class);
        
        public TrustAgentFileTransferManifestProvider(SoftwarePackage softwarePackage, RetrieveLinuxOperatingSystemVersion retrieveLinuxOperatingSystemVersion) {
            this.retrieveLinuxOperatingSystemVersion = retrieveLinuxOperatingSystemVersion;
            this.softwarePackage = softwarePackage;
        }
        
        @Override
        public List<FileTransferDescriptor> getFileTransferManifest() { 
            OperatingSystemInfo data = retrieveLinuxOperatingSystemVersion.getData();
            // copy the installer, marker file, and monitor script...  currently this is a generic step for each package.
            ArrayList<FileTransferDescriptor> packageList = new ArrayList<>();
            List<File> packageInstallerFiles = softwarePackage.getFiles(data.getDistributorName()); 

            if(packageInstallerFiles != null){
                for(File packageInstallerFile: packageInstallerFiles){
                    log.debug("Adding file {} to package {}", packageInstallerFile.getName(), softwarePackage.getPackageName());
                    packageList.add(new FileTransferDescriptor(packageInstallerFile, packageInstallerFile.getName()));
                }
            }
            return packageList;
        }
    }

