/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.jaxrs2.client.JaxrsClient;
import com.intel.mtwilson.jaxrs2.client.JaxrsClientBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 *
 * @author jbuhacoff
 */
public class ImportAttestationServiceCertificatesToKeyBroker extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportAttestationServiceCertificatesToKeyBroker.class);

    public ImportAttestationServiceCertificatesToKeyBroker() {
        super();
    }

    
    @Override
    public void execute() {
        // download .zip file with attestation service certificates
        // requires permission "configuration_databundle:retrieve" in mtwilson
        // NOTE: we use the JaxrsClient from mtwilson-util-jaxrs2-client directly
        // so we don't have a dependency on either attestation service or key broker.
        Properties attestationServiceProperties = new Properties();
        attestationServiceProperties.setProperty("endpoint.url", "https://"+order.getSettings().get("mtwilson.host")+":"+order.getSettings().get("mtwilson.port.https")+"/mtwilson");
        attestationServiceProperties.setProperty("login.basic.username", order.getSettings().get("mtwilson.admin.username"));
        attestationServiceProperties.setProperty("login.basic.password", order.getSettings().get("mtwilson.admin.password"));
        attestationServiceProperties.setProperty("tls.policy.certificate.sha1", order.getSettings().get("mtwilson.tls.cert.sha1"));
        JaxrsClient attestationServiceClient = JaxrsClientBuilder.factory().configuration(attestationServiceProperties).build();
        Response zipResponse = attestationServiceClient.getTargetPath("/v2/configuration/databundle").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();

        byte[] zip;
        try (InputStream zipIn = zipResponse.readEntity(InputStream.class)) {
            zip = IOUtils.toByteArray(zipIn);
            File zipFile = getTaskDirectory().toPath().resolve(String.valueOf(sequence()) + ".attestation_service_config.zip").toFile();
            FileUtils.writeByteArrayToFile(zipFile, zip);
        } catch (IOException e) {
            log.error("Cannot store databundle .zip file", e);
            fault(new Fault("Cannot store attestation service configuration data bundle"));
            return;
        }

        // upload the same .zip file to the key broker
        Entity zipEntity = Entity.entity(zip, MediaType.APPLICATION_OCTET_STREAM);
        FormDataMultiPart zipUpload = new FormDataMultiPart();
        zipUpload.field("file", zipEntity, zipEntity.getMediaType());
        Properties keybrokerProperties = new Properties();
        keybrokerProperties.setProperty("endpoint.url", "https://"+target.getHost()+":"+order.getSettings().get("kms.port.https"));
        keybrokerProperties.setProperty("login.basic.username", order.getSettings().get("kms.admin.username"));
        keybrokerProperties.setProperty("login.basic.password", order.getSettings().get("kms.admin.password"));
        keybrokerProperties.setProperty("tls.policy.certificate.sha1", order.getSettings().get("kms.tls.cert.sha1"));
        JaxrsClient keybrokerClient = JaxrsClientBuilder.factory().configuration(keybrokerProperties).build();
        Response uploadResponse = keybrokerClient.getTargetPath("/v1/databundle").request().post(Entity.entity(zipUpload, zipUpload.getMediaType()));

        MultivaluedMap<String, Object> headersMap = uploadResponse.getHeaders();
        if (log.isDebugEnabled()) {
            log.debug("Response status code: {}", uploadResponse.getStatus());
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
    }
}
