/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.descriptor.NetworkRole;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.CopyQuickstartOrder;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class CopyQuickstartOrderTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopyQuickstartOrderTest.class);
    private static SSH remote;
    
    @BeforeClass
    public static void init() throws IOException {
        Properties properties = PropertiesUtil.removePrefix(Env.getProperties("cit3-test-ssh"), "cit3.test.ssh.");
        remote = new SSH(properties.getProperty("host"), properties.getProperty("password"), properties.getProperty("publicKeyDigest"));
    }

    @Test
    public void testCopyQuickstartOrder() {
        // prepare input
        HashSet<String> packages = new HashSet<>();
        packages.add("attestation_service");
        Target target = new Target();
        target.setPackages(packages);
        target.setHost(remote.getHost());
        target.setUsername(remote.getUsername());
        target.setPassword(remote.getPassword());
        target.setPublicKeyDigest(remote.getPublicKeyDigest());
        SoftwarePackage softwarePackage = new SoftwarePackage("attestation_service", null);
        OrderDocument order = new OrderDocument();
        order.setId(new UUID());
        order.setNetworkRole(NetworkRole.PRIVATE);
        HashSet<String> features = new HashSet<>();
        features.add("vm_encryption");
        order.setFeatures(features);
        HashSet<Target> targets = new HashSet<>();
        targets.add(target);
        order.setTargets(targets);        
        // execute
        CopyQuickstartOrder copyQuickstartOrder = new CopyQuickstartOrder(softwarePackage);
        copyQuickstartOrder.setTarget(target);
        copyQuickstartOrder.setOrderDocument(order);
        copyQuickstartOrder.execute();
    }
}
