/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.performance.AlarmClock;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.jaxrs2.client.JaxrsClient;
import com.intel.mtwilson.jaxrs2.client.JaxrsClientBuilder;
import com.intel.mtwilson.util.validation.faults.Thrown;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 *
 * @author jbuhacoff
 */
public class ImportAttestationServiceCertificatesToKeyBroker extends AbstractPostconfigureTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportAttestationServiceCertificatesToKeyBroker.class);
    private int maxTriesUpload = 1;
    private int progressTriesUpload = 0;
    private DownloadAttestationServiceDataBundle downloadDataBundle = null;
    
    public ImportAttestationServiceCertificatesToKeyBroker() {
        super();
    }

    private JaxrsClient createKeyBrokerClient() {
        Properties keybrokerProperties = new Properties();
        keybrokerProperties.setProperty("endpoint.url", "https://" + target.getHost() + ":" + setting("kms.port.https"));
        keybrokerProperties.setProperty("login.basic.username", setting("kms.admin.username"));
        keybrokerProperties.setProperty("login.basic.password", setting("kms.admin.password"));
        keybrokerProperties.setProperty("tls.policy.certificate.sha256", setting("kms.tls.cert.sha256"));
        JaxrsClient keybrokerClient = JaxrsClientBuilder.factory().configuration(keybrokerProperties).register(MultiPartFeature.class).build();
        return keybrokerClient;
    }


    @Override
    public void execute() {
        downloadDataBundle = new DownloadAttestationServiceDataBundle();
        downloadDataBundle.setOrderDocument(order);
        downloadDataBundle.setTarget(target);
        downloadDataBundle.execute();
        // download .zip file with attestation service certificates
        // requires permission "configuration_databundle:retrieve" in mtwilson
        // NOTE: we use the JaxrsClient from mtwilson-util-jaxrs2-client directly
        // so we don't have a dependency on either attestation service or key broker.
        byte[] zip = downloadDataBundle.getDataBundleZip();
        if (zip == null) {
            return; // faults already logged by downloadDataBundleFromAttestationService
        }

        log.debug("Uploading data bundle to key broker");
        // upload the same .zip file to the key broker
        FormDataContentDisposition disposition = FormDataContentDisposition.name("file").fileName("databundle.tgz").size(zip.length).build();
        FormDataBodyPart body = new FormDataBodyPart(disposition, new ByteArrayInputStream(zip), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        FormDataMultiPart zipUpload = new FormDataMultiPart();
        zipUpload.bodyPart(body);
        JaxrsClient keybrokerClient = createKeyBrokerClient();
        progressTriesUpload = 0;
        Response uploadResponse = keybrokerClient.getTargetPath("/v1/databundle").request().post(Entity.entity(zipUpload, zipUpload.getMediaType()));
        log.debug("Uploaded data bundle to key broker");
        progressTriesUpload = 1;
        
        if (log.isDebugEnabled()) {
            logUploadResponse(uploadResponse);
        }
    }

    private void logUploadResponse(Response uploadResponse) {
        log.debug("Response status code: {}", uploadResponse.getStatus());
        MultivaluedMap<String, Object> headersMap = uploadResponse.getHeaders();
        if (headersMap != null) {
            for (String headerName : headersMap.keySet()) {
                log.debug("Response header name: {}", headerName);
                List<Object> values = headersMap.get(headerName);
                if (values == null) {
                    log.debug("Header value is null");
                } else {
                    for (Object value : values) {
                        log.debug("Header value: {}", value.toString());
                    }
                }
            }
        }

    }

    @Override
    public long getCurrent() {
        return ( downloadDataBundle == null ? 0 : downloadDataBundle.getCurrent() ) + progressTriesUpload;
    }

    @Override
    public long getMax() {
        return ( downloadDataBundle == null ? 0 : downloadDataBundle.getMax() ) + maxTriesUpload;
    }
    
    
}
