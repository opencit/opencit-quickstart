/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.task.RemoteInstall;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class TestDeployInstaller {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestDeployInstaller.class);

    @Test
    public void testDeployInstaller() {
        
    }
    
    @Test
    public void testRunRemoteCommandWithLocalOutput() {
        SSH target = new SSH();
        target.setHost("10.1.68.34");
        target.setPort(22);
        target.setUsername("root");
        target.setPassword("P@ssw0rd");
        target.setPublicKeyDigest("22952a72e24194f208200e76fd3900da");
        RemoteInstall task = new RemoteInstall(target, null, "/bin/ls");
        task.run();
    }
}
