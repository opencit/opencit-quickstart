/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.SshLogin;
import com.intel.mtwilson.deployment.task.FileTransfer;
import com.intel.mtwilson.util.task.ProgressMonitoringTask;
import java.io.File;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This is an integration test... comment out the @Test annotations when not
 * using it.
 * 
 * @author jbuhacoff
 */
public class SshLoginTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SshLoginTest.class);
    private static SSH requestRemoteHostKey, requestValidatePassword;
    
    @BeforeClass
    public static void init() {
        requestRemoteHostKey = new SSH("10.1.68.34", null, null);
        requestValidatePassword = new SSH("10.1.68.34", "P@ssw0rd", "22952a72e24194f208200e76fd3900da");
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
        SshLogin.SshLoginResponse response = login.checkLogin(requestRemoteHostKey);
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
        SshLogin.SshLoginResponse response = login.checkLogin(requestValidatePassword);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginCheckPassword: request: {}", mapper.writeValueAsString(requestValidatePassword));
        log.debug("testSshLoginCheckPassword: response: {}", mapper.writeValueAsString(response));
    }
    

    /**
     * To cache a host ssh key, the user must have already approved it.
     * We know the host key is approved when client sends a password validation request
     * with a specified (approved) host key.
     * @throws JsonProcessingException 
     */
    @Test
    public void testSshLoginGetHostKeyWithCache() throws JsonProcessingException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.set("mtwilson.quickstart.ssh.hostkey.store","true");
        configuration.set("mtwilson.quickstart.ssh.password.store","true");
        SshLogin login = new SshLogin();
        login.setConfiguration(configuration);
        SshLogin.SshLoginResponse response = login.checkLogin(requestRemoteHostKey);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginGetHostKeyWithCache: request: {}", mapper.writeValueAsString(requestRemoteHostKey));
        log.debug("testSshLoginGetHostKeyWithCache: response: {}", mapper.writeValueAsString(response));
    }

    @Test
    public void testSshLoginCheckPasswordWithCache() throws JsonProcessingException {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.set("mtwilson.quickstart.ssh.hostkey.store","true");
        configuration.set("mtwilson.quickstart.ssh.password.store","true");
        SshLogin login = new SshLogin();
        login.setConfiguration(configuration);
        SshLogin.SshLoginResponse response = login.checkLogin(requestValidatePassword);
        ObjectMapper mapper = new ObjectMapper();
        log.debug("testSshLoginCheckPasswordWithCache: request: {}", mapper.writeValueAsString(requestValidatePassword));
        log.debug("testSshLoginCheckPasswordWithCache: response: {}", mapper.writeValueAsString(response));
    }
    
    
}
