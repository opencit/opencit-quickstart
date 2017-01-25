/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;

/**
 * NOTE: adminrc MUST export OS_DEFAULT_DOMAIN  (for EXAMPLE 'default')
 * 
 * @author jbuhacoff
 */
public class PostconfigureOpenstack extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PostconfigureOpenstack.class);
    private SSH remote;
    private static final String JSON_FORMAT = " -f json"; 

    public PostconfigureOpenstack(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

            // create the openstack admin user, following settings must be set in preconfigure task.
            String projectName = setting("openstack.project.name");
            String adminUsername = setting("openstack.admin.username");
            String adminPassword = setting("openstack.admin.password");
            String roleName = "admin";

            
            //Clean up the existing project with the same name if any
            Result projectResult = openstackSilent(client, "project delete " + projectName);
            if (projectResult.getExitCode() == 0) {
                log.info("Deleted the existing project with the name {}. ID that is deleted : {}", projectName);
            } 
            

            //Clean up the existing user with the same name if any
            Result userResult = openstackSilent(client, "user delete " + adminUsername );
            if (userResult.getExitCode() == 0) {
                log.info("Deleted the existing user with the name {}. ID that is deleted : {}", adminUsername);
            } 
            
            //Create project and user
            
            openstack(client, "project create " + projectName + " --description \"Cloud Integrity Technology\" --or-show --domain $OS_DEFAULT_DOMAIN");
            openstack(client, "user create " + adminUsername + " --password " + adminPassword + " --project " + projectName + " --or-show --domain $OS_DEFAULT_DOMAIN");

            // openstack supports "soft" create above with the --or-show option but does not support "soft" role add/remove, 
            // so to avoid errors we must first list the user's roles and only add the admin role if it's not already present
            OpenstackRole[] roles;
            Result roleResult = openstack(client, "role list --project " + projectName + " --user " + adminUsername + JSON_FORMAT);
            if (roleResult.getExitCode() == 0) {
                ObjectMapper mapper = new ObjectMapper();
                roles = mapper.readValue(roleResult.getStdout(), OpenstackRole[].class);
            } else {
                roles = new OpenstackRole[0];
            }
            boolean found = false;
            for (OpenstackRole role : roles) {
                if (roleName.equals(role.name) && adminUsername.equals(role.user) && projectName.equals(role.project)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                openstack(client, "role add "+roleName+" --project " + projectName + " --user " + adminUsername);
            }


        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

    }

    // ConnectionException, TransportException, IOException
    // NOTE: another copy of this in CreateTrustDirectorUserInOpenstack
    private Result openstack(SSHClientWrapper clientWrapper, String command) throws Exception {
        log.debug("Task ID {} openstack {}", getId(), command);
        // escape signle quotes
        String escapedSingleQuoteCommand = command.replace("'", "'\"\\'\"'"); //  f'oo becomes f'"\'"'oo so that when we wrap it in single quotes below it becomes 'f'"\'"'oo' and shell interprets it like concat('f',single quote,'oo')
        Result result = sshexec(clientWrapper, "/bin/bash -c 'source adminrc && /usr/bin/openstack " + escapedSingleQuoteCommand + "'");
        if (result.getExitCode() != 0) {
            String incidentTag = RandomUtil.randomHexString(4);
            log.error("Failed to configure openstack {} [incident tag: {}]", command, incidentTag);
            fault(new Fault("Failed to configure openstack [incident tag: " + incidentTag + "]"));
        }
        return result;
    }

    private Result openstackSilent(SSHClientWrapper clientWrapper, String command) throws Exception {
        log.debug("Task ID {} openstack {}", getId(), command);
        // escape signle quotes
        String escapedSingleQuoteCommand = command.replace("'", "'\"\\'\"'"); //  f'oo becomes f'"\'"'oo so that when we wrap it in single quotes below it becomes 'f'"\'"'oo' and shell interprets it like concat('f',single quote,'oo')
        Result result = sshexec(clientWrapper, "/bin/bash -c 'source adminrc && /usr/bin/openstack " + escapedSingleQuoteCommand + "'");
        return result;
    }
    
    
    public static class OpenstackRole {

        @JsonProperty("ID")
        public String id;
        @JsonProperty("Name")
        public String name;
        @JsonProperty("Project")
        public String project;
        @JsonProperty("User")
        public String user;
    }
    
    

}
