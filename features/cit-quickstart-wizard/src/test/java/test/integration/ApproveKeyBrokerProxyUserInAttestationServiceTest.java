/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.extensions.WhiteboardExtensionProvider;
import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.ApproveKeyBrokerProxyUserInAttestationService;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.io.IOException;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class ApproveKeyBrokerProxyUserInAttestationServiceTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApproveKeyBrokerProxyUserInAttestationServiceTest.class);
    private static SSH keyBrokerProxy;
    private static OrderDocument order;
    
    @BeforeClass
    public static void registerExtensions() {
        // this step only required in junit, the application automatically registers plugins when it launches
        WhiteboardExtensionProvider.register(TlsPolicyCreator.class, CertificateDigestTlsPolicyCreator.class);
    }
    
    @BeforeClass
    public static void environment() throws IOException {
        Properties sshProperties = PropertiesUtil.removePrefix(Env.getProperties("cit3-test-ssh"), "cit3.test.ssh.");
        keyBrokerProxy = new SSH(sshProperties.getProperty("host"), sshProperties.getProperty("password"), sshProperties.getProperty("publicKeyDigest"));
        ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
        order = mapper.readValue(Env.getString("quickstart.json"), OrderDocument.class); // you can generate this quickstart.json file by using the quickstart to deploy into your desired configuration, then use the "export" link in the summary screen (after DONE or ERROR status) to download quickstart.json
    }

    @Test
    public void testApproveKeyBrokerProxyUserInAttestationService() {
        // prepare input
        Target target = new Target();
        target.setHost(keyBrokerProxy.getHost());
        target.setUsername(keyBrokerProxy.getUsername());
        target.setPassword(keyBrokerProxy.getPassword());
        target.setPublicKeyDigest(keyBrokerProxy.getPublicKeyDigest());
        // prepare the task
        ApproveKeyBrokerProxyUserInAttestationService approve = new ApproveKeyBrokerProxyUserInAttestationService(keyBrokerProxy); // the remote passed to constructor is the KEY BROKER PROXY
        approve.setTarget(target); // the target is the ATTESTATION SERVICE
        approve.setOrderDocument(order); // contains the attestation service url, username, and password
        // execute
        approve.execute();
    }
}
