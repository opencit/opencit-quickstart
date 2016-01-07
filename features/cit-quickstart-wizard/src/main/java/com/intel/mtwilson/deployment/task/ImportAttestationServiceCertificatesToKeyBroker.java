/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.jaxrs2.client.JaxrsClient;
import com.intel.mtwilson.jaxrs2.client.JaxrsClientBuilder;
import java.io.ByteArrayInputStream;
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

    public ImportAttestationServiceCertificatesToKeyBroker() {
        super();
    }

    
    @Override
    public void execute() {
        // download .zip file with attestation service certificates
        // requires permission "configuration_databundle:retrieve" in mtwilson
        // NOTE: we use the JaxrsClient from mtwilson-util-jaxrs2-client directly
        // so we don't have a dependency on either attestation service or key broker.
        log.debug("Downloading data bundle from mtwilson");
        Properties attestationServiceProperties = new Properties();
        attestationServiceProperties.setProperty("endpoint.url", "https://"+setting("mtwilson.host")+":"+setting("mtwilson.port.https")+"/mtwilson");
        attestationServiceProperties.setProperty("login.basic.username", setting("mtwilson.quickstart.username"));
        attestationServiceProperties.setProperty("login.basic.password", setting("mtwilson.quickstart.password"));
        attestationServiceProperties.setProperty("tls.policy.certificate.sha1", setting("mtwilson.tls.cert.sha1"));
        JaxrsClient attestationServiceClient = JaxrsClientBuilder.factory().configuration(attestationServiceProperties).build();
        Response zipResponse = attestationServiceClient.getTargetPath("/v2/configuration/databundle").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        log.debug("Downloaded data bundle from mtwilson");

        byte[] zip;
        try (InputStream zipIn = zipResponse.readEntity(InputStream.class)) {
            zip = IOUtils.toByteArray(zipIn);
            File zipFile = getTaskDirectory().toPath().resolve(String.valueOf(sequence()) + ".databundle.tgz").toFile();
            log.debug("Storing databundle.tgz file at {}", zipFile.getAbsolutePath());
            FileUtils.writeByteArrayToFile(zipFile, zip);
        } catch (IOException e) {
            log.error("Cannot store databundle.tgz file", e);
            fault(new Fault("Cannot store attestation service configuration data bundle"));
            return;
        }
        log.debug("Uploading data bundle to key broker");
        // upload the same .zip file to the key broker
        FormDataContentDisposition disposition = FormDataContentDisposition.name("file").fileName("databundle.tgz").size(zip.length).build();
        FormDataBodyPart body = new FormDataBodyPart(disposition, new ByteArrayInputStream(zip), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        FormDataMultiPart zipUpload = new FormDataMultiPart();
        zipUpload.bodyPart(body);
        Properties keybrokerProperties = new Properties();
        keybrokerProperties.setProperty("endpoint.url", "https://"+target.getHost()+":"+setting("kms.port.https"));
        keybrokerProperties.setProperty("login.basic.username", setting("kms.admin.username"));
        keybrokerProperties.setProperty("login.basic.password", setting("kms.admin.password"));
        keybrokerProperties.setProperty("tls.policy.certificate.sha1", setting("kms.tls.cert.sha1"));
        JaxrsClient keybrokerClient = JaxrsClientBuilder.factory().configuration(keybrokerProperties).register(MultiPartFeature.class).build();
        Response uploadResponse = keybrokerClient.getTargetPath("/v1/databundle").request().post(Entity.entity(zipUpload, zipUpload.getMediaType()));
        log.debug("Uploaded data bundle to key broker");

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
