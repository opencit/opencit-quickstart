/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * This is an integration task: prior to installing trust director, a user must
 * be created in key broker for trust director to access those APIs.
 *
 * @author jbuhacoff
 */
public class CreateTrustDirectorUserInKeyBroker extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateTrustDirectorUserInKeyBroker.class);
    private SSH remote;

    /**
     * The remote server to pass here is the Attestation Service,  where we 
     * will ssh to create the director user. 
     * @param remote 
     */
    public CreateTrustDirectorUserInKeyBroker(SSH remote) {
        super();
        this.remote = remote;
    }

    @Override
    public void execute() {

        String directorUsername = setting("director.kms.username"); // TODO:  generate random uuid after it, like kmsproxy does... and this should really be moved to director setup as a registration request w/ mtwilson, so all we should have to do here is get that username from director and then go to mtwilson to approve it.
        if (directorUsername.isEmpty()) {
            directorUsername = "director-" + UUID.randomUUID().toString();
            setting("director.kms.username", directorUsername);
        }
        String directorPassword = setting("director.kms.password");
        if (directorPassword.isEmpty()) {
            int lengthBytes = 16;
            directorPassword = RandomUtil.randomBase64String(lengthBytes).replace("=", "");
            setting("director.kms.password", directorPassword);
        }

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();
                // ensure output directory exists
                File outputDirectory = new File(Folders.repository("tasks") + File.separator + getId());
                outputDirectory.mkdirs();
                log.debug("Output directory: {}", outputDirectory.getAbsolutePath());
                
                // command to execute on attestation service to create the trust director user;  TODO:  if we can just call an API, that would be better than ssh+command.;  see also bug #4866
                // TODO:  escape the director username and password
                // TODO:  refine set of permissions to only what director  actually needs to have
                String cmdCreateTrustDirectorUser = "/opt/kms/bin/kms.sh password " + directorUsername + " " + directorPassword + " --permissions *:*";
                Result result = sshexec(client, cmdCreateTrustDirectorUser);
                if( result.getExitCode() != 0 ) {
                    log.error("Failed to create kms user: {}", directorUsername);
                    fault(new Fault("Cannot create user for Trust Director"));
                }
            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }

        // postcondition:
        // DIRECTOR_MTWILSON_USERNAME and DIRECTOR_MTWILSON_PASSWORD must be set (and corresponding user actually created in mtwilson) 

    }
}
