/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.dcsg.cpg.extensions.WhiteboardExtensionProvider;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.ImportAttestationServiceCertificatesToOpenstack;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class ImportAttestationServiceCertificatesToOpenstackTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportAttestationServiceCertificatesToOpenstackTest.class);
    private static String host;
    private static String openstackPassword;
    private static Map<String,String> settings;
    
    @BeforeClass
    public static void registerExtensions() {
        WhiteboardExtensionProvider.register(TlsPolicyCreator.class, CertificateDigestTlsPolicyCreator.class);
    }
    
    @BeforeClass
    public static void init() throws IOException {
        Properties testProperties = Env.getProperties("openstack");
        
        host = testProperties.getProperty("host");
        openstackPassword = testProperties.getProperty("host.password");
        String mtwilsonTlsCertSha1 = testProperties.getProperty("mtwilson.tls.cert.sha1");
        settings = new HashMap<String,String>();
        settings.put("mtwilson.host", host);
        settings.put("mtwilson.port.https", "8443");
        settings.put("mtwilson.quickstart.username", "quickstart");
        settings.put("mtwilson.quickstart.password", testProperties.getProperty("mtwilson.quickstart.password"));
        settings.put("mtwilson.tls.cert.sha1", mtwilsonTlsCertSha1);
    }
    
    
    /**
     * mtwilson login-password jonathan password --permissions *:*
     * sha1sum /opt/mtwilson/configuration/ssl.crt
     * kms password admin password --permissions *:*
     * cat /opt/kms/configuration/https.properties
     */
    @Test
    public void testTask() {
        OrderDocument order = new OrderDocument();
        order.setSettings(settings);
        Target target = new Target();
        target.setHost(host);
        target.setPublicKeyDigest("22952a72e24194f208200e76fd3900da");
        target.setUsername("root");
        target.setPassword(openstackPassword);
        ImportAttestationServiceCertificatesToOpenstack task = new ImportAttestationServiceCertificatesToOpenstack();
        task.setOrderDocument(order);
        task.setTarget(target);
        task.run();
    }
}
