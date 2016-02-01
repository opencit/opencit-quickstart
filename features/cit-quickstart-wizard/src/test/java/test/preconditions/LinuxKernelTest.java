/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.preconditions;

import com.intel.mtwilson.deployment.LinuxKernelInfoParser;
import com.intel.mtwilson.deployment.LinuxReleaseInfoParser;
import com.intel.mtwilson.deployment.LinuxKernelInfo;
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
public class LinuxKernelTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinuxKernelTest.class);

    @Test
    public void testParseUnameReleaseUbuntu() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/linux/uname_r_ubuntu12.txt")) {
            String unameReleaseText = IOUtils.toString(in);
            LinuxKernelInfoParser parser = new LinuxKernelInfoParser();
            LinuxKernelInfo info = parser.parse(unameReleaseText);
            assertEquals("3.2.0", info.getVersion());
            assertEquals("92-generic", info.getBuild());
        }
        
    }

    @Test
    public void testParseUnameReleaseRedhat() throws IOException {
        try(InputStream in = getClass().getResourceAsStream("/linux/uname_r_redhat7.txt")) {
            String unameReleaseText = IOUtils.toString(in);
            LinuxKernelInfoParser parser = new LinuxKernelInfoParser();
            LinuxKernelInfo info = parser.parse(unameReleaseText);
            assertEquals("3.10.0", info.getVersion());
            assertEquals("123.el7.x86_64", info.getBuild());
        }
        
    }

    @Test
    public void testParseKernelVersion() {
        LinuxKernelInfo info = new LinuxKernelInfo("1.2.3", "generic");
        assertEquals("1", info.getVersionMajor());
        assertEquals("2", info.getVersionMinor());
        assertEquals("3", info.getVersionPatch());
        LinuxKernelInfo info2 = new LinuxKernelInfo("4.5", "generic");
        assertEquals("4", info2.getVersionMajor());
        assertEquals("5", info2.getVersionMinor());
        assertNull(info2.getVersionPatch());
        LinuxKernelInfo info3 = new LinuxKernelInfo("6", "generic");
        assertEquals("6", info3.getVersionMajor());
        assertNull(info3.getVersionMinor());
        assertNull(info3.getVersionPatch());
        LinuxKernelInfo info4 = new LinuxKernelInfo("7.8.9.0", "generic");
        assertEquals("7", info4.getVersionMajor());
        assertEquals("8", info4.getVersionMinor());
        assertEquals("9", info4.getVersionPatch());
        // NOTE: throwing away the final .0 
    }
}
