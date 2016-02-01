/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.LinuxKernelInfo;
import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.SshLogin;
import com.intel.mtwilson.deployment.task.RetrieveLinuxKernelVersion;
import com.intel.mtwilson.deployment.task.RetrieveLinuxOperatingSystemVersion;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDigestVerifier;
import java.io.IOException;
import java.util.Properties;
import net.schmizz.sshj.SSHClient;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is an integration test... comment out the
 *
 * @Test annotations when not using it.
 *
 * @author jbuhacoff
 */
public class SshLoginTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SshLoginTest.class);
    private static SshLogin.HostPrecheckRequest requestRemoteHostKey, requestValidatePassword;

    @BeforeClass
    public static void init() throws IOException {
        Properties properties = PropertiesUtil.removePrefix(Env.getProperties("cit3-test-ssh"), "cit3.test.ssh.");
        requestRemoteHostKey = new SshLogin.HostPrecheckRequest(properties.getProperty("host"), null, null);
        requestValidatePassword = new SshLogin.HostPrecheckRequest(properties.getProperty("host"), properties.getProperty("password"), properties.getProperty("publicKeyDigest"));
    }

    /**
     * Example request:
     * <pre>
     * {"host":"10.1.68.34","port":22,"username":"root","password":null,"publicKeyDigest":null,"timeout":15000}
     * </pre>
     *
     * Example response:
     * <pre>
     * {"data":{"host":"10.1.68.34","port":22,"username":"root","password":null,"publicKeyDigest":"22952a72e24194f208200e76fd3900da","timeout":15000}}
     * </pre>
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSshLoginGetHostKey() throws JsonProcessingException {
        SshLogin login = new SshLogin();
        SshLogin.HostPrecheckResponse response = login.checkLogin(requestRemoteHostKey);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginGetHostKey: request: {}", mapper.writeValueAsString(requestRemoteHostKey));
        log.debug("testSshLoginGetHostKey: response: {}", mapper.writeValueAsString(response));
    }

    /**
     * Example request (real password represented by ********):
     * <pre>
     *  {"host":"10.1.68.34","port":22,"username":"root","password":"********","publicKeyDigest":"22952a72e24194f208200e76fd3900da","timeout":15000}
     * </pre>
     *
     * Example response:
     * <pre>
     * {"data":{"host":"10.1.68.34","port":22,"username":"root","password":null,"publicKeyDigest":"22952a72e24194f208200e76fd3900da","timeout":15000}}
     * </pre>
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSshLoginCheckPassword() throws JsonProcessingException {
        SshLogin login = new SshLogin();
        SshLogin.HostPrecheckResponse response = login.checkLogin(requestValidatePassword);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginCheckPassword: request: {}", mapper.writeValueAsString(requestValidatePassword));
        log.debug("testSshLoginCheckPassword: response: {}", mapper.writeValueAsString(response));
    }

    /**
     * To cache a host ssh key, the user must have already approved it. We know
     * the host key is approved when client sends a password validation request
     * with a specified (approved) host key.
     *
     * @throws JsonProcessingException
     */
    @Test
    public void testSshLoginGetHostKeyWithCache() throws JsonProcessingException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.set("mtwilson.quickstart.ssh.hostkey.store", "true");
        configuration.set("mtwilson.quickstart.ssh.password.store", "true");
        SshLogin login = new SshLogin();
        login.setConfiguration(configuration);
        SshLogin.HostPrecheckResponse response = login.checkLogin(requestRemoteHostKey);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginGetHostKeyWithCache: request: {}", mapper.writeValueAsString(requestRemoteHostKey));
        log.debug("testSshLoginGetHostKeyWithCache: response: {}", mapper.writeValueAsString(response));
    }

    @Test
    public void testSshLoginCheckPasswordWithCache() throws JsonProcessingException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.set("mtwilson.quickstart.ssh.hostkey.store", "true");
        configuration.set("mtwilson.quickstart.ssh.password.store", "true");
        SshLogin login = new SshLogin();
        login.setConfiguration(configuration);
        SshLogin.HostPrecheckResponse response = login.checkLogin(requestValidatePassword);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginCheckPasswordWithCache: request: {}", mapper.writeValueAsString(requestValidatePassword));
        log.debug("testSshLoginCheckPasswordWithCache: response: {}", mapper.writeValueAsString(response));
    }

    @Test
    public void testRetrieveLinuxOperatingSystemVersion() throws Exception {
        Target target = new Target();
        target.setHost(requestValidatePassword.getHost());
        target.setPort(requestValidatePassword.getPort());
        target.setUsername(requestValidatePassword.getUsername());
        target.setPassword(requestValidatePassword.getPassword());
        target.setPublicKeyDigest(requestValidatePassword.getPublicKeyDigest());

        RetrieveLinuxOperatingSystemVersion task = new RetrieveLinuxOperatingSystemVersion();
        task.setTarget(target);
        task.execute();

        OperatingSystemInfo info = task.getData();
        if (info != null) {
            log.debug("Got distribution {} version {}", info.getDistributor(), info.getVersion());
            // example: Got distribution Ubuntu version 14.04
        } else {
            log.error("Failed to retrieve operating system info");
        }
    }

    @Test
    public void testRetrieveLinuxKernelVersion() throws Exception {
        Target target = new Target();
        target.setHost(requestValidatePassword.getHost());
        target.setPort(requestValidatePassword.getPort());
        target.setUsername(requestValidatePassword.getUsername());
        target.setPassword(requestValidatePassword.getPassword());
        target.setPublicKeyDigest(requestValidatePassword.getPublicKeyDigest());

        RetrieveLinuxKernelVersion task = new RetrieveLinuxKernelVersion();
        task.setTarget(target);
        task.execute();

        LinuxKernelInfo info = task.getData();
        if (info != null) {
            log.debug("Got version {} build {}", info.getVersion(), info.getBuild());
            // example:  Got version 3.13.0 build 24-generic
        } else {
            log.error("Failed to retrieve kernel info");
        }
    }
}
