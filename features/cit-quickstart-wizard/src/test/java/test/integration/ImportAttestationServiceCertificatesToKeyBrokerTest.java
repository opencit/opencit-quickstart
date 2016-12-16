/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.dcsg.cpg.extensions.WhiteboardExtensionProvider;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.deployment.task.ImportAttestationServiceCertificatesToKeyBroker;
import com.intel.mtwilson.jaxrs2.client.JaxrsClient;
import com.intel.mtwilson.jaxrs2.client.JaxrsClientBuilder;
import com.intel.mtwilson.tls.policy.creator.impl.CertificateDigestTlsPolicyCreator;
import com.intel.mtwilson.tls.policy.factory.TlsPolicyCreator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class ImportAttestationServiceCertificatesToKeyBrokerTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportAttestationServiceCertificatesToKeyBrokerTest.class);
    private static String host;
    private static Map<String,String> settings;
    
    @BeforeClass
    public static void registerExtensions() {
        WhiteboardExtensionProvider.register(TlsPolicyCreator.class, CertificateDigestTlsPolicyCreator.class);
    }
    
    @BeforeClass
    public static void init() {
        host = "10.1.68.31";
        String mtwilsonTlsCertSha256 = "915916e6c44e80b3977392641e0ee92cb296104d4c17d593731ffc45cd6cf9cc";
        String keybrokerTlsCertSha256 = "915916e6c44e80b3977392641e0ee92cb296104d4c17d593731ffc45cd6cf9cc";
        settings = new HashMap<>();
        settings.put("mtwilson.host", host);
        settings.put("mtwilson.port.https", "8443");
        settings.put("mtwilson.quickstart.username", "quickstart");
        settings.put("mtwilson.quickstart.password", "8Byr+SjR0C9KxsX1JYIivQ");
        settings.put("mtwilson.tls.cert.sha256", mtwilsonTlsCertSha256);
        settings.put("kms.port.https", "20443");
        settings.put("kms.admin.username", "admin");
        settings.put("kms.admin.password", "00NRWepfflHMWMvnvVcBuA");
        settings.put("kms.tls.cert.sha256", keybrokerTlsCertSha256);
    }
    
    // method present in the task being tested so copy/paste is easier for the code
    private String setting(String key) {
        return settings.get(key);
    }
    
    private byte[] readTestZip() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/mtwilson_databundle.tgz")) {
            return IOUtils.toByteArray(in);
        }
    }
    
    /**
     * This tests the first part of the ImportAttestationServiceCertificatesToKeyBroker
     * execute() method
     */
    @Test
    public void testDownloadDataBundleFromAttestationService() {
        // test code
        Properties attestationServiceProperties = new Properties();
        attestationServiceProperties.setProperty("endpoint.url", "https://"+setting("mtwilson.host")+":"+setting("mtwilson.port.https")+"/mtwilson");
        attestationServiceProperties.setProperty("login.basic.username", setting("mtwilson.quickstart.username"));
        attestationServiceProperties.setProperty("login.basic.password", setting("mtwilson.quickstart.password"));
        attestationServiceProperties.setProperty("tls.policy.certificate.sha256", setting("mtwilson.tls.cert.sha256"));
        JaxrsClient attestationServiceClient = JaxrsClientBuilder.factory().configuration(attestationServiceProperties).build();
        Response zipResponse = attestationServiceClient.getTargetPath("/v2/configuration/databundle").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
        log.debug("Downloaded data bundle from mtwilson");

        byte[] zip;
        try (InputStream zipIn = zipResponse.readEntity(InputStream.class)) {
            zip = IOUtils.toByteArray(zipIn);
            log.debug("zip file length: {}", zip.length);
            File zipFile = new File(Folders.repository("tasks") + File.separator + "databundle.tgz");
            log.debug("Storing databundle.tgz file at {}", zipFile.getAbsolutePath());
            FileUtils.writeByteArrayToFile(zipFile, zip);
        } catch (IOException e) {
            log.error("Cannot store databundle.tgz file", e);
        }
    }
    
    /**
     * If you get this error:
     * <pre>
     * org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException: MessageBodyWriter not found for media type=multipart/form-data, type=class org.glassfish.jersey.media.multipart.FormDataMultiPart, genericType=class org.glassfish.jersey.media.multipart.FormDataMultiPart.
     * </per>
     * You have to make sure you have this dependency:
     * <pre>
     * <groupId>org.glassfish.jersey.media</groupId>
       <artifactId>jersey-media-multipart</artifactId>
     * </pre>
     * And that the client does this:
     * <pre>
     * .register(MultiPartFeature.class)
     * </pre>
     * For example:
     * <pre>
     * Client client = ClientBuilder.newBuilder()
     * .register(MultiPartFeature.class)
     * .build();
     * </pre>
     * @throws IOException 
     */
    @Test
    public void testImportDataBundleToKeyBroker() throws IOException {
        // setup
        Target target = new Target();
        target.setHost(host);
        byte[] zip = readTestZip(); // throws IOException 
        // test code
        FormDataContentDisposition disposition = FormDataContentDisposition.name("file").fileName("databundle.tgz").size(zip.length).build();
        FormDataBodyPart body = new FormDataBodyPart(disposition, new ByteArrayInputStream(zip), MediaType.APPLICATION_OCTET_STREAM_TYPE);
        FormDataMultiPart zipUpload = new FormDataMultiPart();
        zipUpload.bodyPart(body);
        Properties keybrokerProperties = new Properties();
        keybrokerProperties.setProperty("endpoint.url", "https://"+target.getHost()+":"+setting("kms.port.https"));
        keybrokerProperties.setProperty("login.basic.username", setting("kms.admin.username"));
        keybrokerProperties.setProperty("login.basic.password", setting("kms.admin.password"));
        keybrokerProperties.setProperty("tls.policy.certificate.sha256", setting("kms.tls.cert.sha256"));
        JaxrsClient keybrokerClient = JaxrsClientBuilder.factory().configuration(keybrokerProperties).register(MultiPartFeature.class).build();
        Response uploadResponse = keybrokerClient.getTargetPath("/v1/databundle").request().post(Entity.entity(zipUpload, zipUpload.getMediaType()));
        log.debug("Uploaded data bundle to key broker");
        
    }
    
    @Test
    public void testShowClasspath() {
        String classpath = System.getProperty("java.class.path");
        log.debug("classpath: {}", classpath);
        log.debug("path separator: {}", File.pathSeparator);
        String[] entries = StringUtils.split(classpath, System.getProperty("path.separator")); 
        for(String entry : entries) {
            log.debug("entry: {}", entry);
        }
    }
    
    /**
     * mtwilson login-password jonathan password --permissions *:*
     * sha256sum /opt/mtwilson/configuration/ssl.crt
     * kms password admin password --permissions *:*
     * cat /opt/kms/configuration/https.properties
     */
    @Test
    public void testTask() {
        OrderDocument order = new OrderDocument();
        order.setSettings(settings);
        Target target = new Target();
        target.setHost(host);
        ImportAttestationServiceCertificatesToKeyBroker task = new ImportAttestationServiceCertificatesToKeyBroker();
        task.setOrderDocument(order);
        task.setTarget(target);
        task.run();
    }
}
