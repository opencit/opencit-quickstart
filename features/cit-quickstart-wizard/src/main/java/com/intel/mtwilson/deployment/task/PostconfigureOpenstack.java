/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;

/**
 *
 * @author jbuhacoff
 */
public class PostconfigureOpenstack extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureOpenstack.class);
    private SSH remote;

    public PostconfigureOpenstack(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();

            // create the openstack admin user, following settings must be set in preconfigure task.
            String projectName = setting("openstack.project.name");
            String adminUsername = setting("openstack.admin.username");
            String adminPassword = setting("openstack.admin.password");
            
            openstack(client, "project create "+projectName+" --description \"Cloud Integrity Technology\" --or-show");
            openstack(client, "user create "+adminUsername+" --password "+adminPassword+" --project "+projectName+" --or-show");
            openstack(client, "role add admin --project "+projectName+" --user "+adminUsername);

            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

    }

    // NOTE: another copy of this in CreateTrustDirectorUserInOpenstack
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
