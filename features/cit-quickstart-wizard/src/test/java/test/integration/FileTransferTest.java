/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.task.FileTransfer;
import com.intel.mtwilson.util.task.ProgressMonitoringTask;
import java.io.File;
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
    public static void init() {
        remote = new SSH("10.1.68.34", "P@ssw0rd", "22952a72e24194f208200e76fd3900da");
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
