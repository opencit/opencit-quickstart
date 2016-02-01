/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.preconditions;

import com.intel.mtwilson.deployment.LinuxReleaseInfoParser;
import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.jaxrs.SshLogin;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jbuhacoff
 */
public class LinuxReleaseTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinuxReleaseTest.class);

    @Test
    public void testParseLsbReleaseUbuntu() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/linux/lsb_release_ubuntu12.txt")) {
            String lsbReleaseText = IOUtils.toString(in);
            LinuxReleaseInfoParser parser = new LinuxReleaseInfoParser();
            OperatingSystemInfo info = parser.parse(lsbReleaseText);
            assertEquals("Ubuntu", info.getDistributor());
            assertEquals("12.04", info.getVersion());
        }
        
    }

    @Test
    public void testParseLsbReleaseRedhat() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/linux/lsb_release_redhat7.txt")) {
            String lsbReleaseText = IOUtils.toString(in);
            LinuxReleaseInfoParser parser = new LinuxReleaseInfoParser();
            OperatingSystemInfo info = parser.parse(lsbReleaseText);
            assertEquals("RedHatEnterpriseServer", info.getDistributor());
            assertEquals("7.0", info.getVersion());
        }
        
    }

}
