/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.task.FileTransfer;
import com.intel.mtwilson.util.task.ProgressMonitoringTask;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This is an integration test... comment out the @Test annotations when not
 * using it.
 * 
 * @author jbuhacoff
 */
public class FileTransferTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileTransferTest.class);
    private static SSH remote;
    private static File source;
    
    @BeforeClass
    public static void init() throws IOException {
        Properties properties = PropertiesUtil.removePrefix(Env.getProperties("cit3-test-ssh"), "cit3.test.ssh.");
        remote = new SSH(properties.getProperty("host"), properties.getProperty("password"), properties.getProperty("publicKeyDigest"));
        source = new File(Folders.repository("packages")+File.separator+"attestation_service"+File.separator+"mtwilson.env.st4");
    }
    
    @Test
    public void testSftp() {
        FileTransfer sftp = new FileTransfer(remote, new FileTransferDescriptor(source));
        ProgressMonitoringTask monitor = new ProgressMonitoringTask(sftp);
        monitor.run();
        assertEquals(source.length(), sftp.getCurrent());
    }
}
