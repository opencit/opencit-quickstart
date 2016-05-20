/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.core.data.bundle.Namespace;
import com.intel.mtwilson.core.data.bundle.TarGzipBundle;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * The target of this task is the openstack controller
 * 
 * @author jbuhacoff
 */
public class ImportAttestationServiceCertificatesToOpenstack extends AbstractPostconfigureTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportAttestationServiceCertificatesToOpenstack.class);
    public static final String MTWILSON_CONFIGURATION_NAMESPACE = "com.intel.mtwilson.configuration";
    public static final String TPM_IDENTITY_FILENAME = "TLS.pem";
    private DownloadAttestationServiceDataBundle downloadDataBundle = null;
    private FileTransfer fileTransfer = null;
    
    public ImportAttestationServiceCertificatesToOpenstack() {
        super();
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
        
        log.debug("Data bundle size: {}", zip.length);
        
        // extract the TLS certificates from the bundle
        File taskDirectory = getTaskDirectory();
        if (!taskDirectory.exists()) {
            taskDirectory.mkdirs();
        }
        
        log.debug("Task directory: {}  exists? {}", taskDirectory.getAbsolutePath(), taskDirectory.exists());
        File pemFile = taskDirectory.toPath().resolve(String.valueOf(sequence()) + "." + TPM_IDENTITY_FILENAME).toFile(); // something like /opt/cit/repository/tasks/{id}/TLS.pem
        log.debug("Store PEM file at: {}  writable? {}", pemFile.getAbsolutePath(), pemFile.canWrite());
        TarGzipBundle bundle = new TarGzipBundle();
        log.debug("Trying to read bundle");
        try(ByteArrayInputStream in = new ByteArrayInputStream(zip)) {
            log.debug("Reading bundle");
            bundle.read(in);
            Namespace namespace = bundle.namespace(MTWILSON_CONFIGURATION_NAMESPACE);
            if (namespace.contains(TPM_IDENTITY_FILENAME)) {
                byte[] pem = namespace.get(TPM_IDENTITY_FILENAME); // UTF-8 bytes, like new String(pem, Charset.forName("UTF-8"))
                if( pem == null ) {
                    fault(new Fault("Failed to extract TLS certificates from data bundle"));
                    return;
                }
                // store it in our task directory so we can pass it to the file transfer task
                log.debug("Storing {} at {}", TPM_IDENTITY_FILENAME,  pemFile.getAbsolutePath());
                FileUtils.writeByteArrayToFile(pemFile, pem);
            }
        }
        catch(IOException e) {
            log.error("Cannot read data bundle", e);
            fault(new Fault("Cannot read data bundle"));
            return;
        }

        
        log.debug("Uploading TLS certificate to OpenStack");
        FileTransferDescriptor fileTransferDescriptor = new FileTransferDescriptor(pemFile, "/etc/nova/ssl.crt"); // this value is currently hard-coded into openstack extensions installer; it would be more descriptive to call it /etc/nova/attestation_server_tls.pem
        fileTransfer = new FileTransfer(target, fileTransferDescriptor); // target must be the openstack controller
        fileTransfer.execute();
        log.debug("Upload complete");
    }

    @Override
    public long getCurrent() {
        return ( downloadDataBundle == null ? 0 : downloadDataBundle.getCurrent() ) + ( fileTransfer == null ? 0 : fileTransfer.getCurrent() );
    }

    @Override
    public long getMax() {
        return Math.max(1, ( downloadDataBundle == null ? 0 : downloadDataBundle.getMax() ) + ( fileTransfer == null ? 0 : fileTransfer.getMax() ));
    }
    
}
