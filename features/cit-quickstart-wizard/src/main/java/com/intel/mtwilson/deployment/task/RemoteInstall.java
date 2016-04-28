/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.dcsg.cpg.performance.AlarmClock;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runs an installer on a remote host
 *
 * @author jbuhacoff
 */
public class RemoteInstall extends AbstractRemoteTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RemoteInstall.class);
    private SSH remote;
    private String executablePath;
    private SoftwarePackage softwarePackage;

    public RemoteInstall(SSH remote, SoftwarePackage softwarePackage) {
        super();
        this.remote = remote;
        this.softwarePackage = softwarePackage;
        //RemoteInstall is set to just work for default installer. Need to update following code to support OS specific installer 
        if(softwarePackage.getFiles("default") != null)
            this.executablePath = softwarePackage.getFiles("default").get(0).getName();
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

            /*
            String chmod = "/bin/chmod +x " + executablePath;
            Result chmodResult = sshexec(client, chmod);
            if (chmodResult.getExitCode() != 0) {
                log.warn("chmod failed on host: {} file: {}", remote.getHost(), executablePath);
            }
            */

            //Result installResult = sshexec(client, "./"+executablePath+" 1>"+executablePath+".out 2>"+executablePath+".err &", 1, TimeUnit.MINUTES); // give 1 minutes max for the install because it will be in background... we'll login later to check status!
            // this should return right away since we are starting it in the background.
            String id = RandomUtil.randomHexString(8);
            String workingDirectory = "/tmp/cit/monitor/"+id;
            Result installResult = sshexec(client, "/bin/bash monitor.sh "+executablePath+" "+executablePath+".mark "+workingDirectory+" >/dev/null &"); // the redirection to /dev/null is so that we won't get the console text-based progress bar, which would hold up our connection and prevent us from then checking the progress output files below.
            if (installResult.getExitCode() != 0) {
                log.error("Install failed on host: {}  file: {}", remote.getHost(), executablePath);
            }
            
            AlarmClock delay = new AlarmClock(1, TimeUnit.SECONDS);
            
            // first wait until the monitor script has parsed the marker file
            // and created the "status" and "max" files. we only need to get "max" once.
            String progressMax = null;
            while( progressMax == null || progressMax.isEmpty() ) {
                log.debug("getting progress max...");
                Result progressMaxResult = sshexec(client, "/bin/cat "+workingDirectory+File.separator+"max");
                progressMax = progressMaxResult.getStdout();
                if( progressMax != null && !progressMax.isEmpty() ) {
                    log.debug("updating progress max: {}", progressMax);
                    max(Integer.valueOf(progressMax.trim()));
                    break;
                }
                delay.sleep();
            }
            
            // now get progress updates periodically while we wait for the installer to finish            
            // which will be marked by status 'DONE' or 'ERROR' or 'CANCELLED'
            String status = null;
            String progress;
            while( status == null || "PENDING".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status) ) {
                log.debug("getting progress...");
                Result progressResult = sshexec(client, "/bin/cat "+workingDirectory+File.separator+"progress");
                progress = progressResult.getStdout();
                if( progress != null && !progress.isEmpty() ) {
                    log.debug("updating progress: {}", progress);
                    current(Integer.valueOf(progress.trim()));
                }
                Result statusResult = sshexec(client, "/bin/cat "+workingDirectory+File.separator+"status");
                status = statusResult.getStdout();
                if( status != null && !status.isEmpty() ) {
                    status = status.trim();
                    log.debug("got status: {}", status);
                }
                else {
                    log.debug("status is null or empty");
                }
                delay.sleep();
            }
            
            
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
