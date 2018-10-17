/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.LinuxKernelInfo;
import com.intel.mtwilson.deployment.LinuxKernelInfoParser;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.TargetAware;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.util.exec.Result;
import java.io.IOException;

/**
 *
 * <pre>
 * uname -o   =>   GNU/Linux
 * uname -s   =>   Linux
 * uname -r   =>   3.10.0-123.el7.x86_64  or  3.2.0-92-generic
 * uname -v   =>   #1 SMP Mon May 5 11:16:57 EDT 2014
 * uname -p   =>   x86_64
 * uname -i   =>   x86_64
 * </pre>
 */
public class RetrieveLinuxKernelVersion extends AbstractRemoteTask implements TargetAware {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetrieveLinuxKernelVersion.class);
    private Target target;
    private LinuxKernelInfo data;

    public RetrieveLinuxKernelVersion() {
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
    
    private LinuxKernelInfo retrieve(SSHClientWrapper client) {
        String cmdGetVersionInfo = "/bin/uname -r";
        try {
            Result result = sshexec(client, cmdGetVersionInfo);
            String unameInfo = result.getStdout();
            LinuxKernelInfoParser parser = new LinuxKernelInfoParser();
            LinuxKernelInfo info = parser.parse(unameInfo);
            return info;
        } catch (Exception e) {
            log.error("Cannot execute lsb_release on remote host: {}", target.getHost(), e);
            return null;
        }
    }

    public LinuxKernelInfo getData() {
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
