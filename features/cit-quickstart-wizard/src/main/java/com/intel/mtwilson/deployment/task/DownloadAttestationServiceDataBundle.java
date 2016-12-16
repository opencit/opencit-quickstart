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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author jbuhacoff
 */
public class DownloadAttestationServiceDataBundle extends AbstractPostconfigureTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DownloadAttestationServiceDataBundle.class);
    private int maxTriesDownload = 10;
    private int progressTriesDownload = 0;
    private int downloadRetryIntervalSeconds = 10;
    private byte[] dataBundleZip;
    
    public DownloadAttestationServiceDataBundle() {
        super();
    }

    private JaxrsClient createAttestationServiceClient() {
        Properties attestationServiceProperties = new Properties();
        attestationServiceProperties.setProperty("endpoint.url", "https://" + setting("mtwilson.host") + ":" + setting("mtwilson.port.https") + "/mtwilson");
        attestationServiceProperties.setProperty("login.basic.username", setting("mtwilson.quickstart.username"));
        attestationServiceProperties.setProperty("login.basic.password", setting("mtwilson.quickstart.password"));
        attestationServiceProperties.setProperty("tls.policy.certificate.sha256", setting("mtwilson.tls.cert.sha256"));
        JaxrsClient attestationServiceClient = JaxrsClientBuilder.factory().configuration(attestationServiceProperties).build();
        return attestationServiceClient;
    }

    // note: code duplicated from ImportAttestationServiceCertificatesToKeyBroker; need to refactor.
    private byte[] downloadDataBundleFromAttestationService() {
        log.debug("Downloading data bundle from mtwilson");
        JaxrsClient attestationServiceClient = createAttestationServiceClient();
        Response zipResponse = null;

        // the loop here is because sometimes mtwilson is not accessible in the short time
        // after it is installed, so if we get a connection error we just try again a few
        // times and it's likely to work eventually if it did install without errors.
        // we keep track of the faults in a temporary list, so if we eventually succeed we
        // discard them, but if we exhaust max attempts then we log all the faults
        progressTriesDownload = 0;
        ArrayList<Fault> faults = new ArrayList<>();
        while (zipResponse == null && progressTriesDownload < maxTriesDownload) {
            try {
                log.debug("Downloading data bundle from mtwilson, attempt {}", progressTriesDownload+1); // +1 because we haven't completed that attempt yet
                zipResponse = attestationServiceClient.getTargetPath("/v2/configuration/databundle").request().accept(MediaType.APPLICATION_OCTET_STREAM).get();
                log.debug("Downloaded data bundle from mtwilson");
            } catch (Exception e) {
                log.error("Cannot download data bundle from mtwilson", e);
                faults.add(new Thrown(e));
                AlarmClock clock = new AlarmClock();
                clock.sleep(downloadRetryIntervalSeconds, TimeUnit.SECONDS);
            }
            progressTriesDownload++;
        }
        if (zipResponse == null) {
            log.error("Cannot download data bundle from mtwilson after {} attempts", progressTriesDownload);
            getFaults().addAll(faults); // log all collected faults
            return null;
        }
        progressTriesDownload = maxTriesDownload; // since we successfully downloaded it, we reflect this in progress 

        File taskDirectory = getTaskDirectory();
        if (!taskDirectory.exists()) {
            taskDirectory.mkdirs();
        }
        
        byte[] zip;
        try (InputStream zipIn = zipResponse.readEntity(InputStream.class)) {
            zip = IOUtils.toByteArray(zipIn);
            File zipFile = taskDirectory.toPath().resolve(String.valueOf(sequence()) + ".databundle.tgz").toFile();
            log.debug("Storing databundle.tgz file at {}", zipFile.getAbsolutePath());
            FileUtils.writeByteArrayToFile(zipFile, zip);
        } catch (IOException e) {
            log.error("Cannot store databundle.tgz file", e);
            fault(new Fault("Cannot store attestation service configuration data bundle"));
            return null;
        }
        
        log.debug("Data bundle size: {}", zip.length);

        return zip;
    }

    @Override
    public void execute() {
        // download .zip file with attestation service certificates
        // requires permission "configuration_databundle:retrieve" in mtwilson
        // NOTE: we use the JaxrsClient from mtwilson-util-jaxrs2-client directly
        // so we don't have a dependency on either attestation service or key broker.
        dataBundleZip = downloadDataBundleFromAttestationService();
        if (dataBundleZip == null) {
            return; // faults already logged by downloadDataBundleFromAttestationService
        }

        log.debug("Downloaded data bundle from Attestation Service");
    }

    /**
     * Must call execute() and check that there are no faults before 
     * calling this and using return value. 
     * @return bytes for zip file containing data bundle, or null if not downloaded or error during download
     */
    public byte[] getDataBundleZip() {
        return dataBundleZip;
    }
        

    @Override
    public long getCurrent() {
        return progressTriesDownload;
    }

    @Override
    public long getMax() {
        return maxTriesDownload;
    }
        
}
