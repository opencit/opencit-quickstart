/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.io.ByteArray;
import com.intel.mtwilson.util.ssh.SshUtils;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * 
 * @author jbuhacoff
 */
public class TestRemoteHostKeyDigest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestRemoteHostKeyDigest.class);
  
    
    @Test
    public void testEncodeRSAPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        // the following public key base64 is in the Java RSA public key encoding format
        String publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3n0ZgkP9jXaPsSeJDlh9lwUXGq2qgueM0yKV3eLnIM5utUxwSY6CR099TOSiw8xQYxAGibLDqB5RX9KuZbXowTijzjC3CReBTFhQROZ+P/JLEABEUQcveUwAuSlhxygcxntq/mdOf5leJaNS3Dh+bisGZx9vJUx6Ba8p707pa5NPjv72d7aviT/rz9uPE+4Go0OBtyw5hmzf/meQgRmrWcTD8ENQamaeRjUFpierXD/KpjDM6cVFXpcYPdBcq1pG/lIEmq9w5cWheM/8gOMNv6/z1OEPAsUexBUmoLc5uXwiDZe3XI24Afdbf1mH287bgIEYRjk3zF/vmhgF/SxqwQIDAQAB";
        byte[] publicKeyBytes = Base64.decodeBase64(publicKeyBase64);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        byte[] opensshBytes = SshUtils.encodeSshRsaPublicKey((RSAPublicKey)publicKey);
        log.debug("Public key OpenSSH format: {}", opensshBytes);
        String opensshBytesMd5 = Digest.md5().digest(opensshBytes).toHex();
        log.debug("Public key MD5: {}", opensshBytesMd5 );
        assertEquals("22952a72e24194f208200e76fd3900da", opensshBytesMd5);
    }
    
    @Test
    public void contentOfUninitializedByteArray() {
        byte[] zero = new byte[8];
        Arrays.fill(zero, (byte)0x00);
        byte[] uninitialized = new byte[8];
        log.debug("zero padding uninitialized: {}", Hex.encodeHexString(uninitialized));
        assertArrayEquals(zero, uninitialized);
    }

}
