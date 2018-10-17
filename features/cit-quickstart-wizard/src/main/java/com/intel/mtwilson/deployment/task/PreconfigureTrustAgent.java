/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.OperatingSystemInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 * Generates trustagent.env using a template. 
 * 
 * @author jbuhacoff
 */
public class PreconfigureTrustAgent extends AbstractPreconfigureTask implements FileTransferManifestProvider {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreconfigureTrustAgent.class);
    private List<FileTransferDescriptor> manifest;
    private File envFile;
    private RetrieveLinuxOperatingSystemVersion retrieveLinuxOperatingSystemVersion;
    
    /**
     * Initializes the task with a file transfer manifest; the file(s) mentioned
     * in the manifest will not be available until AFTER execute() completes
     * successfully.
     * @param retrieveLinuxOperatingSystemVersion
     */
    public PreconfigureTrustAgent(RetrieveLinuxOperatingSystemVersion retrieveLinuxOperatingSystemVersion) {
        super(); // initializes taskDirectory
        envFile = getTaskDirectory().toPath().resolve("mtwilson-openstack.env").toFile();
        manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(envFile, envFile.getName()));
        this.retrieveLinuxOperatingSystemVersion = retrieveLinuxOperatingSystemVersion;
    }
    
    @Override
    public void execute() {
        // preconditions:  
        // MTWILSON_HOST, MTWILSON_PORT, and MTWILSON_TLS_CERT_SHA256 must be set ;  note that if using a load balanced mtwilson, the tls cert is for the load balancer
        // the host and port are set by PreconfigureAttestationService, but the tls sha256 fingerprint is set by PostconfigureAttestationService.
        // either way, the sync task forces all attestation service tasks to complete before key broker proxy tasks start, so these settings should be present.
        OperatingSystemInfo osInfo = retrieveLinuxOperatingSystemVersion.getData();
        if( setting("mtwilson.host").isEmpty() || setting("mtwilson.port.https").isEmpty() || setting("mtwilson.tls.cert.sha256").isEmpty() ) {
            throw new IllegalStateException("Missing required settings"); // TODO:  rewrite as a precondition
        }
        // the PreconfigureAttestationService task must already be executed 
        data.put("MTWILSON_HOST", setting("mtwilson.host"));
        data.put("MTWILSON_PORT", setting("mtwilson.port.https"));
        // the PostconfigureAttestationService task must already be executed 
        data.put("MTWILSON_TLS_CERT_SHA256", setting("mtwilson.tls.cert.sha256"));

        // preconditions:
        // TRUSTAGENT_MTWILSON_USERNAME and TRUSTAGENT_MTWILSON_PASSWORD must be set,  these are created by CreateTrustAgentUserInAttestationService.... 
        data.put("TRUSTAGENT_MTWILSON_USERNAME", setting("trustagent.mtwilson.username"));
        data.put("TRUSTAGENT_MTWILSON_PASSWORD", setting("trustagent.mtwilson.password"));
        
        
        // trustagent settings:  TODO:  looks like env file doesn't include customziing the trustagent port
        port();
        data.put("JETTY_PORT", setting("trustagent.port.http"));
        data.put("JETTY_SECURE_PORT", setting("trustagent.port.https"));
        
        // optional:
        // IF key broker proxy is installed, then we need its settings 
        data.put("KMSPROXY_HOST", setting("kmsproxy.host"));
        data.put("KMSPROXY_PORT", setting("kmsproxy.port.http"));  // NOTE:  when trustagent uses key broker proxy, it uses http not https ; see CIT 3.0 architecture
        
        // optional: 
        // if docker feature was selected, then we need to set this variable to 'docker'; if vm attestation was selected, then we need to set this to 'vm', otherwise it can be empty string
        String deploymentType = "";
        if( order != null ) {
            Set<String> features = order.getFeatures();
            if( features != null && features.contains("attestation_container") ) {
                deploymentType = "docker";
            }
            else if( features != null && features.contains("attestation_vm") ) {
                deploymentType = "vm";
            }
        }
        data.put("DEPLOYMENT_TYPE", deploymentType);
          
        data.put("TRUSTAGENT_HOST", target.getHost());
        switch(osInfo.getDistributorName()){
            case "ubuntu":
                data.put("GRUB_FILE", "/boot/grub/grub.cfg");
                break;
            case "redhat":
                data.put("GRUB_FILE", "/boot/grub2/grub.cfg");
                break;
        }
        
        // generate the .env file using pre-configuration data
        render("mtwilson-openstack.env.st4", envFile);
    }

    private void port() {
        // if the target has more than one software package to be installed on it,
        // use our alternate port
        if (setting("trustagent.port.http").isEmpty() ||setting("trustagent.port.https").isEmpty()) {
            // TODO:  the port conflict check for trustagent should not be based on how many packages WE are installing... because tehre may be already be other software on that node;  that's why the default is 1443 already.
            if (target.getPackages().size() == 1) {
                setting("trustagent.port.http", "1081");  // the default trustagent http port, not known to be used by any common software
                setting("trustagent.port.https", "1443"); // the default trustagent https port, not known to be used by any common software
            } else {
                setting("trustagent.port.http", "17080");
                setting("trustagent.port.https", "17443");
            }
        }
    }


    @Override
    public String getPackageName() {
        return "trustagent";
    }

    /**
     * Must be called AFTER execute() to get list of files that should be
     * transferred to the remote host
     * @return 
     */
    @Override
    public List<FileTransferDescriptor> getFileTransferManifest() {
        return manifest;
    }

    
}
