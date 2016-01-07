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
"director.tls.cert.sha1": "",
"director.glance.host": "10.1.68.31",
"director.glance.port": "9292",
"director.glance.username": "admin",
"director.glance.password": "password",
"director.glance.tenant": "admin",
"kms.host": "",
"kms.port.http": "20080",
"kms.port.https": "20443",
"kms.tls.cert.sha1": "15bd8afa40e6a7d0ceb0942a9794805c706e93fe",
"kms.key.provider": "com.intel.kms.barbican.client.BarbicanKeyManager",
"kms.kmip.url": "",
"kms.barbican.url": "http://10.1.68.31:9311",
"kms.barbican.project": "admin",
"kmsproxy.host": "",
"kmsproxy.port.http": "",
"kmsproxy.port.https": "",
"mtwilson.host": "10.1.68.31",
"mtwilson.port.http": "8080",
"mtwilson.port.https": "8443",
"mtwilson.tls.cert.sha1": "4ec09d750dd0e7d85d75ffdcfa4621191775d6f8",
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
