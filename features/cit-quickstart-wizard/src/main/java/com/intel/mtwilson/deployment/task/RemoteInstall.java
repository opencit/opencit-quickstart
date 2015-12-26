/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.task.AbstractTask;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Runs an installer on a remote host
 *
 * @author jbuhacoff
 */
public class RemoteInstall extends AbstractTaskWithId {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RemoteInstall.class);
    private SSH remote;
    private String executablePath;
    private SoftwarePackage softwarePackage;

    public RemoteInstall(SSH remote, SoftwarePackage softwarePackage) {
        super();
        this.remote = remote;
        this.softwarePackage = softwarePackage;
        this.executablePath = softwarePackage.getFile().getName();
    }
    public RemoteInstall(SSH remote, SoftwarePackage softwarePackage, String executablePath) {
        super();
        this.remote = remote;
        this.softwarePackage = softwarePackage;
        this.executablePath = executablePath;
    }

    @Override
    public void execute() {
        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();
            try (Session session = client.session()) {

                Command command = session.exec("/bin/ls " + executablePath); // TODO ....    INSTALL IT.
                InputStream stdout = command.getInputStream();
                InputStream stderr = command.getErrorStream();
                String stdoutText = IOUtils.toString(stdout, "UTF-8");
                String stderrText = IOUtils.toString(stderr, "UTF-8");
                log.debug("result: {}", stdoutText);

                // ensure output directory exists
                File outputDirectory = new File(Folders.repository("tasks") + File.separator + getId());
                outputDirectory.mkdirs();
                log.debug("Output directory: {}", outputDirectory.getAbsolutePath());
                
                // store the stdout into a file
                File stdoutFile = new File(Folders.repository("tasks") + File.separator + getId() + File.separator + "stdout.log");                
                FileUtils.writeStringToFile(stdoutFile, stdoutText, Charset.forName("UTF-8"));
                
                // store the stderr into a file
                File stderrFile = new File(Folders.repository("tasks") + File.separator + getId() + File.separator + "stderr.log");                
                FileUtils.writeStringToFile(stderrFile, stderrText, Charset.forName("UTF-8"));
            }
            client.disconnect();
        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }
    }

    public String getPackageName() {
        return softwarePackage.getPackageName();
    }
    
    public String getHost() {
        return remote.getHost();
    }
    
    public String getExecutablePath() {
        return executablePath;
    }
    
    
}
