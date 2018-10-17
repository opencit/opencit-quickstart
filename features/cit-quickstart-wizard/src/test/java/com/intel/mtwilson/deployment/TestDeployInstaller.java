/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.task.RemoteInstall;
import java.io.IOException;
import java.util.Properties;
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
    public void testRunRemoteCommandWithLocalOutput() throws IOException {
        Properties properties = PropertiesUtil.removePrefix(Env.getProperties("cit3-test-ssh"), "cit3.test.ssh.");
        
        SSH target = new SSH();
        target.setHost(properties.getProperty("host"));
        target.setPort(22);
        target.setUsername(properties.getProperty("username"));
        target.setPassword(properties.getProperty("password"));
        target.setPublicKeyDigest(properties.getProperty("publicKeyDigest"));
        RemoteInstall task = new RemoteInstall(target, null, "/bin/ls");
        task.run();
    }
}
