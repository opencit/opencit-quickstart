/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.io.ByteArray;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDeferredVerifier;
import com.intel.mtwilson.util.ssh.SshUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import net.schmizz.sshj.SSHClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * 
 * @author jbuhacoff
 */
public class TestSshClient {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestSshClient.class);
  
    // THIS IS AN INTEGRATION TEST,  COMMENTED OUT UNTIL THERE IS A FRAMEWORK
    // FOR INJECTING THE RIGHT RESOURCES (IN THIS CASE , THE SSH-ENABLED HOST
    // IP ADDRESS LIKE 192.168.1.100)
//    @Test
    public void testTimeoutForConnection() throws NoSuchAlgorithmException, InvalidKeySpecException {
        int timeout = 2000; // 2000 milliseconds = 2 seconds
        String host = "192.168.1.100"; 
        int port = 22;
        String username = "root";
        
        RemoteHostKeyDeferredVerifier hostKeyVerifier = new RemoteHostKeyDeferredVerifier();
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(hostKeyVerifier); // this accepts all remote public keys, then you have to verify the remote host key before continuing!!!
            ssh.setTimeout(30000); // in milliseconds; connection attempt will be dropped if idle longer than this amount of time to connect
            ssh.setConnectTimeout(timeout); // in milliseconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
            ssh.connect(host, port);
            ssh.authPassword(username, ""); // we don't actually send the password when we're just trying to get the remote host public key
        } catch (IOException e) {
            log.debug("Connection failed", e); // we expect it to fail because we provided an empty password
        }
        log.debug("Remote host key for {}", hostKeyVerifier.getRemoteHostKey().getHost());
    }
    
}
