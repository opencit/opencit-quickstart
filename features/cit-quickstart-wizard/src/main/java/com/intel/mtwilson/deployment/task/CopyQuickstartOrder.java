/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.OrderUtils;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.util.exec.Result;
import com.intel.mtwilson.util.validation.faults.Thrown;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Used to copy the order (without ssh passwords) to each Attestation Service
 * and Trust Director so that in the future, an upgrade-capable Quickstart
 * would be able to ask the user for the hostname & password of any already-
 * installed Attestation Service or Trust Director, download the order file,
 * and then pre-populate the user interface with the settings. 
 * The user would then be able to make adjustments, such as configuring new
 * features or adding new trust agent hosts. 
 * 
 * @author jbuhacoff
 */
public class CopyQuickstartOrder extends AbstractPostconfigureTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopyQuickstartOrder.class);
    private final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
    private final String softwarePackageName;
    
    /**
     * Must use setTarget and setOrder to provide those objects
     * prior to calling execute
     * @param softwarePackage 
     */
    public CopyQuickstartOrder(SoftwarePackage softwarePackage) {
        this.softwarePackageName = softwarePackage.getPackageName();
    }
    
    @Override
    public void execute() {
        // make a sanitized copy of "order"
        try {
            File orderFile = createOrderFile();
            // ssh to "target" and copy the order file
            String remotePath = getRemotePath();
            copyOrderToRemotePath(orderFile, remotePath);
        }
        catch(IOException e) {
            log.debug("Cannot copy order file to remote system: {}", target.getHost(), e);
            fault(new Thrown(e));
        }
    }
    
    private String getRemotePath() {
        if( "attestation_service".equals(softwarePackageName)) {
            return "/opt/mtwilson/configuration/quickstart";
        }
        if( "director".equals(softwarePackageName)) {
            return "/opt/director/configuration/quickstart";
        }
        return "/tmp/quickstart";
    }
    
    private File createOrderFile() throws IOException {
        // filename is "order-{date}" 
        SimpleDateFormat iso8601abbrev = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");  // for example: 20160427T163112Z  or 20160427T163112-0700
        String filename = "order-" + iso8601abbrev.format(new Date());
        File orderFile = getTaskDirectory().toPath().resolve(filename).toFile();
        if( !orderFile.getParentFile().exists() ) {
            log.debug("Attempting to create directory {}", orderFile.getParentFile().getAbsolutePath());
            orderFile.getParentFile().mkdirs();
        }
        OrderDocument clean = OrderUtils.sanitize(order);
        clean.setSettings(null); // intentionally remove all settings, because these may have changed post-deployment and anyway current settings can be retrieved from any installed server when user has the right credentials
        clean.setStatus(null); // remove the status, it's not needed for importing an order file later for upgrade
        clean.setTasks(null); // remove the tasks, they are not needed for importing an order file later for upgrade
        clean.getLinks().clear(); // remove the links, currently they are for status, cancel, and export, and are relative links, so they are useless later.
        mapper.writeValue(orderFile, clean);
        return orderFile;
    }
    
    private void copyOrderToRemotePath(File orderFile, String remotePath) {
        // copy the file to remote host
        ArrayList<FileTransferDescriptor> manifest = new ArrayList<>();
        manifest.add(new FileTransferDescriptor(orderFile, orderFile.getName()));
        FileTransfer fileTransfer = new FileTransfer(target, manifest);
        fileTransfer.execute();
        try (SSHClientWrapper client = new SSHClientWrapper(target)) {
            // create directory on remote host 
            Result mkdirResult = sshexec(client, "/bin/mkdir -p "+remotePath);
            if( mkdirResult.getExitCode() == 0 ) {
                // move the order file to directory
                Result mvResult = sshexec(client, "/bin/mv "+orderFile.getName()+" "+remotePath);
                if( mvResult.getExitCode() == 0 ) {
                    log.debug("copyOrderToRemotePath done");
                }
                else {
                    log.error("Failed to move order file to: {}", remotePath);
                    fault(new Fault("Failed to move order file"));
                }
            }
            else {
                fault(new Fault("Failed to create remote directory to store order files"));
            }
        } catch (Exception e) {
            log.error("Cannot connect to {}", target.getHost(), e);
        }
    }
    
    
}
