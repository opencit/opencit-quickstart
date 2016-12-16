/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.ImportAttestationServiceCertificatesToKeyBroker;
import com.intel.mtwilson.launcher.ws.ext.V2;
import java.util.HashMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/quickstart/test")
public class TestImportAttestationServiceCertificatesToKeyBroker {
    
    /**
     * Example request:
     * <pre>
{
"target": "10.1.68.31",
"settings": {
"saml.validity.seconds": "900",
"director.port.http": "",
"director.port.https": "",
"director.tls.cert.sha256": "",
"director.glance.url": "http://10.1.68.31:9292",
"director.keystone.url": "http://10.1.68.31:5000",
"director.glance.username": "admin",
"director.glance.password": "password",
"director.glance.tenant": "admin",
"kms.host": "",
"kms.port.http": "20080",
"kms.port.https": "20443",
"kms.tls.cert.sha256": "915916e6c44e80b3977392641e0ee92cb296104d4c17d593731ffc45cd6cf9cc",
"kms.key.provider": "com.intel.kms.barbican.client.BarbicanKeyManager",
"kms.kmip.url": "",
"kms.barbican.project": "admin",
"kms.barbican.url": "http://10.1.68.31:9311",
"kms.keystone.url": "http://10.1.70.112:5000",
"kms.barbican.username": "barbican",
"kms.barbican.password": "password",
"kms.barbican.tenant": "service",
"kmsproxy.host": "",
"kmsproxy.port.http": "",
"kmsproxy.port.https": "",
"mtwilson.host": "10.1.68.31",
"mtwilson.port.http": "8080",
"mtwilson.port.https": "8443",
"mtwilson.tls.cert.sha256": "06180e0116b8106b2e75d8eb43d11cd0f365418a61d946bce109944902de63b3",
"director.mtwilson.username": "director-585006f6-95d6-4204-a4b2-988ec6217c85",
"director.mtwilson.password": "QC5GI6yivIlSkGoFgb19oA",
"mtwilson.tag.provision.xml.password": "TY0ZsH8p7wxM/dX2DhvghA",
"mtwilson.admin.password": "/0pzyDHgksplQbaShPz7ZA",
"mtwilson.quickstart.username": "quickstart",
"mtwilson.tag.admin.password": "Uk896vzR/NAS/OlTlqDvDQ",
"mtwilson.privacyca.download.password": "ycrxGQvJ6viQQbpjm8PvWw",
"mtwilson.tag.admin.username": "tagadmin",
"mtwilson.database.password": "upG66YMFugCW9Hr/4wU2sQ",
"mtwilson.quickstart.password": "8Byr+SjR0C9KxsX1JYIivQ",
"mtwilson.admin.username": "admin",
"mtwilson.privacyca.download.username": "admin",
"kms.admin.username": "admin",
"kms.admin.password": "00NRWepfflHMWMvnvVcBuA"
}
}
     * 
     * </pre>
     * 
     * Response is just echo of request object, possibly with faults or other status.
     * @param testDocument
     * @return 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TestDocument test(TestDocument testDocument) {
        OrderDocument order = new OrderDocument();
        order.setSettings(testDocument.settings);
        Target target = new Target();
        target.setHost(testDocument.target);
        ImportAttestationServiceCertificatesToKeyBroker task = new ImportAttestationServiceCertificatesToKeyBroker();
        task.setOrderDocument(order);
        task.setTarget(target);
        task.run();
        return testDocument;
    }
    
    public static class TestDocument {
        public String target;
        public HashMap<String,String> settings = new HashMap<>();
    }
}
