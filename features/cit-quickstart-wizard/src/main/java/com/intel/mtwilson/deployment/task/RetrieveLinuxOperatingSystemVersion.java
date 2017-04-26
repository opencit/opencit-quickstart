/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.LinuxReleaseInfoParser;
import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.TargetAware;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.util.exec.Result;
import java.io.IOException;

/**
 * Retrieves Linux distributor and release version
 *
 * @author jbuhacoff
 */
public class RetrieveLinuxOperatingSystemVersion extends AbstractRemoteTask implements TargetAware {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetrieveLinuxOperatingSystemVersion.class);
    private Target target;
    private OperatingSystemInfo data;

    public RetrieveLinuxOperatingSystemVersion() {
        super();
    }

    @Override
    public void execute() {
        try (SSHClientWrapper client = new SSHClientWrapper(target)) {
            data = retrieve(client);
        } catch (IOException e) {
            log.error("Cannot connect to {}", target.getHost(), e);
        }
    }
    
    private OperatingSystemInfo retrieve(SSHClientWrapper client) {
        String cmdGetVersionInfo = "cat /etc/*release";
        try {
            Result result = sshexec(client, cmdGetVersionInfo);
            String lsbReleaseInfo = result.getStdout();
            LinuxReleaseInfoParser parser = new LinuxReleaseInfoParser();
            OperatingSystemInfo info = parser.parse(lsbReleaseInfo);
            return info;
        } catch (Exception e) {
            log.error("Cannot execute cat /etc/*release on remote host: {}", target.getHost(), e);
            return null;
        }
    }

    public OperatingSystemInfo getData() {
        return data;
    }
    
    public String getHost() {
        return target.getHost();
    }

    @Override
    public void setTarget(Target target) {
        this.target = target;
    }
}
