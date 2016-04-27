/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.user.management.client.jaxrs.UserLoginCertificates;
import com.intel.mtwilson.user.management.client.jaxrs.Users;
import com.intel.mtwilson.user.management.rest.v2.model.Status;
import com.intel.mtwilson.user.management.rest.v2.model.User;
import com.intel.mtwilson.user.management.rest.v2.model.UserCollection;
import com.intel.mtwilson.user.management.rest.v2.model.UserFilterCriteria;
import com.intel.mtwilson.user.management.rest.v2.model.UserLoginCertificate;
import com.intel.mtwilson.user.management.rest.v2.model.UserLoginCertificateCollection;
import com.intel.mtwilson.user.management.rest.v2.model.UserLoginCertificateFilterCriteria;
import com.intel.mtwilson.util.exec.Result;
import com.intel.mtwilson.util.validation.faults.Thrown;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This is an integration task: when installing key broker proxy, it
 * automatically creates and registers a user in Attestation Service. We need to
 * approve that user.
 *
 * The username to approve should be stored in our settings by the
 * KeyBrokerProxyPostConfiguration task, which reads it from the remote
 * configuration.
 *
 * @author jbuhacoff
 */
public class ApproveKeyBrokerProxyUserInAttestationService extends AbstractPostconfigureTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CreateTrustDirectorUserInAttestationService.class);
    private SSH remote;

    /**
     * The remote server to pass here is the Key Broker Proxy, where we will ssh
     * to find out what is the key broker proxy username to approve.
     *
     * @param remote
     */
    public ApproveKeyBrokerProxyUserInAttestationService(SSH remote) {
        this.remote = remote;
    }

    @Override
    public void execute() {

        String keyBrokerProxyUsername = setting("kmsproxy.mtwilson.username");
        if (keyBrokerProxyUsername.isEmpty()) {
            // not provided by user, so retrieve it from the key broker proxy configuration
            try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
                // strip newlines or whitespace from the key broker proxy username
                keyBrokerProxyUsername = readRemoteConfig(client, "mtwilson.username").trim();
            } catch (Exception e) {
                log.error("Connection failed", e);
                fault(new Connection(remote.getHost()));
                return;
            }
        }

        if (keyBrokerProxyUsername == null || keyBrokerProxyUsername.isEmpty()) {
            fault(new Fault("Cannot approve key broker proxy user, username unknown"));
            return;
        }

        // mtwilson connection settings
        Properties attestationServiceClientProperties = createAttestationServiceClientProperties();
        
        // first, search for the key broker proxy user by username
        // second, search for a corresponding user login certificate
        // third,  update the user login certificate record in mtwilson with the approved roles

        try {
            log.debug("Searching for key broker proxy user in attestation service");
            UserFilterCriteria userFilter = new UserFilterCriteria();
            userFilter.nameEqualTo = keyBrokerProxyUsername;
            Users users = new Users(attestationServiceClientProperties);
            UserCollection userCollection = users.searchUsers(userFilter);
            log.debug("Completed search for key broker proxy user in mtwilson, got {} results", userCollection.getUsers().size());

            ArrayList<String> keyBrokerProxyRoles = new ArrayList<>();
            keyBrokerProxyRoles.add("Attestation"); // this is the only role the key broker proxy needs

            for (User user : userCollection.getUsers()) {
                log.debug("Found user: {} with id: {}", user.getUsername(), user.getId().toString());
                UserLoginCertificateFilterCriteria userLoginCertificateFilter = new UserLoginCertificateFilterCriteria();
                userLoginCertificateFilter.userUuid = user.getId();
                userLoginCertificateFilter.status = Status.PENDING;
                UserLoginCertificates userLoginCertificates = new UserLoginCertificates(attestationServiceClientProperties);
                UserLoginCertificateCollection userLoginCertificateCollection = userLoginCertificates.searchUserLoginCertificates(userLoginCertificateFilter);
                log.debug("Completed search for key broker proxy user login certificate in mtwilson, got {} results", userLoginCertificateCollection.getUserLoginCertificates().size());
                for (UserLoginCertificate userLoginCertificate : userLoginCertificateCollection.getUserLoginCertificates()) {
                    log.debug("Approving user login certificate with subject: {}", userLoginCertificate.getX509Certificate().getSubjectX500Principal().getName());
                    userLoginCertificate.setRoles(keyBrokerProxyRoles);
                    userLoginCertificate.setEnabled(true);
                    userLoginCertificate.setStatus(Status.APPROVED);
                    userLoginCertificates.editUserLoginCertificate(userLoginCertificate);
                }
            }
        } catch (Exception e) {
            fault(new Thrown(e));
        }


    }

    // ConnectionException, TransportException, IOException
    private String readRemoteConfig(SSHClientWrapper clientWrapper, String key) throws Exception {
        Result result = sshexec(clientWrapper, "/opt/kmsproxy/bin/kmsproxy.sh config " + key);
        if (result.getExitCode() != 0) {
            log.error("Failed to read key broker configuration for key: {}", key);
            fault(new Fault("Failed to read " + key));
            return null;
        } else {
            log.info("Key broker configuration for key {}", key);
            return result.getStdout();
        }
    }

    private Properties createAttestationServiceClientProperties() {
        Properties attestationServiceProperties = new Properties();
        attestationServiceProperties.setProperty("endpoint.url", "https://" + setting("mtwilson.host") + ":" + setting("mtwilson.port.https") + "/mtwilson/v2");
        attestationServiceProperties.setProperty("login.basic.username", setting("mtwilson.quickstart.username"));
        attestationServiceProperties.setProperty("login.basic.password", setting("mtwilson.quickstart.password"));
        attestationServiceProperties.setProperty("tls.policy.certificate.sha1", setting("mtwilson.tls.cert.sha1"));
        return attestationServiceProperties;
    }
}
