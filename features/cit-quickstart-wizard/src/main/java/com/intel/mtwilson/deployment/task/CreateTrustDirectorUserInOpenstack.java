/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.transport.TransportException;

/**
 * 
 * 
 * @author jbuhacoff
 */
public class CreateTrustDirectorUserInOpenstack extends AbstractPostconfigureTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateTrustDirectorUserInOpenstack.class);
    private SSH remote;
    
    /**
     */
    public CreateTrustDirectorUserInOpenstack(SSH remote) {
        super(); // initializes taskDirectory
        this.remote = remote;
    }
    
    @Override
    public void execute() {
        
        // the project name, username, and password are used in PostconfigureOpenstack
        String openstackProjectName = setting("director.glance.tenant");
        String directorUsername = setting("director.glance.username"); 
        String directorPassword = setting("director.glance.password");
        
        // precondition: proejct name, username, and password must be defined
        if( openstackProjectName.isEmpty() || directorUsername.isEmpty() || directorPassword.isEmpty() ) {
            throw new IllegalArgumentException("Missing Glance tenant name, user name, or password");
        }
        
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();

            openstack(client, "project create "+openstackProjectName+" --description \"Cloud Integrity Technology\" --or-show");
            openstack(client, "user create "+directorUsername+" --password "+directorPassword+" --project "+openstackProjectName+" --or-show");
            openstack(client, "role add admin --project "+openstackProjectName+" --user "+directorUsername);
        
            
            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }
    }

    // NOTE: another copy of this in PostconfigureOpenstack
    private void openstack(SSHClientWrapper clientWrapper, String command) throws ConnectionException, TransportException, IOException {
        // escape signle quotes
        String escapedSingleQuoteCommand = command.replace("'", "'\"\\'\"'"); //  f'oo becomes f'"\'"'oo so that when we wrap it in single quotes below it becomes 'f'"\'"'oo' and shell interprets it like concat('f',single quote,'oo')
        Result result = sshexec(clientWrapper, "/bin/bash -c 'source adminrc && /usr/bin/openstack " + escapedSingleQuoteCommand + "'");
        if (result.getExitCode() != 0) {
            String incidentTag = RandomUtil.randomHexString(4);
            log.error("Failed to configure openstack {} [incident tag: {}]", command, incidentTag);
            fault(new Fault("Failed to configure openstack [incident tag: "+incidentTag+"]"));
        }
    }

}
