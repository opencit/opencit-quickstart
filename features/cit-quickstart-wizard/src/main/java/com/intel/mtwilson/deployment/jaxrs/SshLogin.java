/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.intel.mtwilson.deployment.OperatingSystemInfo;
import com.intel.mtwilson.deployment.LinuxKernelInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.crypto.RandomUtil;
import com.intel.mtwilson.deployment.jaxrs.faults.Null;
import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.crypto.key.password.Password;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.dcsg.cpg.validation.Faults;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.crypto.password.HashedPassword;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.core.PasswordVaultFactory;
import com.intel.mtwilson.crypto.password.PasswordUtil;
import com.intel.mtwilson.deployment.LinuxKernelInfoParser;
import com.intel.mtwilson.deployment.LinuxReleaseInfoParser;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.deployment.jaxrs.faults.ConnectionTimeout;
import com.intel.mtwilson.deployment.jaxrs.faults.NoRouteToHost;
import com.intel.mtwilson.deployment.task.AbstractRemoteTask;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.util.crypto.keystore.PasswordKeyStore;
import com.intel.mtwilson.util.exec.Result;
import com.intel.mtwilson.util.ssh.RemoteHostKey;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDeferredVerifier;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDigestVerifier;
import com.intel.mtwilson.util.ssh.SshUtils;
import com.intel.mtwilson.util.task.Condition;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import org.apache.commons.lang.StringUtils;

/**
 * Call without public key and password to just get the remote host's public key
 * and return it
 *
 * Call with public key and password to attempt login to remote host and report
 * success or failure
 *
 * Add a "packages" array to the request and the server will attempt to detect
 * if the packages can be installed on the specified host by checking for
 * pre-requisites.  The server only checks pre-requisites if it knows how to
 * check pre-requisites for a given software package.  It is not an error if
 * the server does not have a pre-requisites check for a specified package, but
 * the server MAY return a fault if the package is not known at all.
 * 
 * @author jbuhacoff
 */
@V2
@Path("/rpc/ssh-login")
public class SshLogin {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SshLogin.class);
    private Configuration configuration;
    private boolean hostKeyStoreEnabled = false, passwordStoreEnabled = false;
    private File hostKeyFile, passwordFile;
//    private PasswordKeyStore hostKeyStore, passwordStore;
//    private Password hostKeyStorePassword, passwordStorePassword;
    private HostKeyCollection hostKeys;
    private HashedPasswordCollection passwords;

    /**
     * HOW TO OBTAIN A REMOTE HOST'S SSH PUBLIC KEY FINGERPRINT: Request
     * example:
     * <pre>
     * POST https://cit.example.com/v1/rpc/ssh-login
     * Content-Type: application/json
     *
     * {
     *   "host":"192.168.1.100"
     * }
     * </pre>
     *
     * Response example:
     * <pre>
     * {
     *   "data":{
     *     "host":"192.168.1.100",
     *     "port":22,
     *     "username":"root",
     *     "public_key_digest":"22952a72e24194f208200e76fd3900da"
     *   }
     * }
     * </pre>
     *
     *
     * HOW TO CHECK PASSWORD FOR SSH LOGIN TO REMOTE HOST: Request example:
     * <pre>
     * https://cit.example.com/v1/rpc/ssh-login
     * Content-Type: application/json
     *
     * {
     *   "host":"192.168.1.100",
     *   "public_key_digest":"22952a72e24194f208200e76fd3900da",
     *   "password":"password to test"
     * }
     * </pre>
     *
     * Example success response (password is correct -- but notice password is
     * not included in output):
     * <pre>
     * {
     *   "data":{
     *     "host":"192.168.1.100",
     *     "port":22,
     *     "username":"root",
     *     "public_key_digest":"22952a72e24194f208200e76fd3900da"
     *   }
     * }
     * </pre>
     *
     * Example failure response (password is not correct -- but notice password
     * is not included in output):
     * <pre>
     * {
     *   "faults":[
     *     {
     *       "type":"com.intel.mtwilson.deployment.jaxrs.faults.Connection",
     *       "description":"Exhausted available authentication methods"
     *     }
     *   ],
     *   "data":{
     *     "host":"192.168.1.100",
     *     "port":22,
     *     "username":"root",
     *     "public_key_digest":"22952a72e24194f208200e76fd3900da"
     *   }
     * }
     * </pre>
     *
     * Example failure response (host public key doesn't match provided public
     * key fingerprint):
     * <pre>
     * {
     *   "faults":[
     *     {
     *       "type":"com.intel.mtwilson.deployment.jaxrs.faults.Connection",
     *       "description":"Could not verify `ssh-rsa` host key with fingerprint `22:95:2a:72:e2:41:94:f2:08:20:0e:76:fd:39:00:da` for `10.1.68.33` on port 22"
     *     }
     *   ],
     *   "data":{
     *     "host":"10.1.68.33",
     *     "port":22,
     *     "username":"root",
     *     "public_key_digest":"22952a72e24194f208200e76fd3900da"
     *   }
     * }
     * </pre>
     *
     * Example server timeout response (cannot connect to specified host):
     * <pre>
     * {
     *   "faults":[
     *     {
     *       "type":"com.intel.mtwilson.deployment.jaxrs.faults.ConnectionTimeout",
     *       "description":"Connection to 10.1.68.33 timeout after 30 seconds",
     *       "host": 10.1.68.33,
     *       "timeout":000000
     *     }
     *   ],
     *   "data":{
     *     "host":"10.1.68.33",
     *     "port":22,
     *     "username":"root",
     *     "public_key_digest":"22952a72e24194f208200e76fd3900da",
     *   }
     * }
     * </pre>
     *
     * HOW TO CHECK PASSWORD FOR SSH LOGIN TO REMOTE HOST AND PACKAGE PRE-REQUISITES: Request example:
     * <pre>
     * https://cit.example.com/v1/rpc/ssh-login
     * Content-Type: application/json
     *
     * {
     *   "host":"192.168.1.100",
     *   "public_key_digest":"22952a72e24194f208200e76fd3900da",
     *   "password":"password to test",
     *   "packages": [ "attestation_service", "director" ]
     * }
     * 
     * </pre>
     *
     * 
     * @param loginRequest
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HostPrecheckResponse checkLogin(HostPrecheckRequest loginRequest) {
        HostPrecheckResponse response = new HostPrecheckResponse();
        String host = loginRequest.getHost();
        Integer port = loginRequest.getPort();
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String publicKeyDigest = loginRequest.getPublicKeyDigest();
        Integer timeout = loginRequest.getTimeout(); // milliseconds

        if (host == null) {
            response.getFaults().add(new Null("host"));
        }
        if (port == null) {
            port = 22;
        }
        if (username == null) {
            username = "root";
        }
        if (timeout == null) {
            timeout = 3000; // 3 second default
        }
//        if( password == null ) { password = null; }
//        if( publicKeyDigest == null ) { publicKeyDigest = null; }

        SSH target = new SSH();
        target.setHost(host);
        target.setPort(port);
        target.setUsername(username);
        target.setPassword(null);
        target.setPublicKeyDigest(publicKeyDigest);
        target.setTimeout(timeout);

        // assume we're going to have an error so echo user input (to help client handle asynchronous responses to multiple concurrent requests)
//        response.setExtra(target);

        if (!response.getFaults().isEmpty()) {
            return response;
        }

        if (configuration == null) {
            loadConfiguration();
        }

        // confirm it, then create a new SSH object without the password to return
        if (publicKeyDigest == null) {

            // if public host key caching is enabled, look in our cache first.
            if (hostKeyStoreEnabled) {
                log.debug("host key store is enabled, checking host key");
                String existing = getHostKey(host);
                if (existing != null) {
                    target.setPublicKeyDigest(existing);
                    response.setData(target);
                    return response;
                }
            }

            RemoteHostKeyDeferredVerifier hostKeyVerifier = new RemoteHostKeyDeferredVerifier();
            try (SSHClient ssh = new SSHClient()) {
                ssh.addHostKeyVerifier(hostKeyVerifier); // this accepts all remote public keys, then you have to verify the remote host key before continuing!!!
                ssh.setConnectTimeout(timeout); // in milliseconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
                ssh.connect(host, port);
                ssh.authPassword(username, ""); // we don't actually send the password when we're just trying to get the remote host public key
                // shouldn't get here because we expect authentication to fail
                log.warn("Login successful to remote server without a password");
                // check pre-requisites, if applicable
                if( !loginRequest.getPackages().isEmpty() ) {
                    // TODO: check pre-reqs!
                    log.debug("Checking pre-requisites for packages: {}", StringUtils.join(loginRequest.packages, ", "));
                }
                // pass through to getRemoteHostKey() below
            } catch (SocketTimeoutException e) {
                log.debug("Connection to {} timeout after {}ms: {}", host, timeout, e.getMessage());
                response.getFaults().add(new ConnectionTimeout(host, timeout));
                return response;
            } catch (ConnectException e) {
                log.debug("Connection failed with ConnectException: {}", e.getMessage());
                String message = e.getMessage();
                if (message != null && message.contains("Connection timed out")) {
                    response.getFaults().add(new ConnectionTimeout(host, timeout));
                } else {
                    response.getFaults().add(new Connection(host));
                }
                return response;
            } catch (NoRouteToHostException e) {
                log.debug("Connection failed with NoRouteToHostException: {}", e.getMessage());
                response.getFaults().add(new NoRouteToHost(host));
                return response;
            } catch (TransportException e) {
                log.debug("Connection failed with TransportException: {}", e.getMessage());
                response.getFaults().add(new Connection(host));
                return response;
            } catch (UserAuthException e) {
                // this exception is expected because we provided an empty password
                log.debug("Caught expected UserAuthException: {}", e.getMessage());
                // pass through to getRemoteHostKey() below
            } catch (IOException e) {
                log.debug("Connection failed with IOException: {}", e.getMessage());
                response.getFaults().add(new Connection(host));
                return response;
            }

            /*
             log.debug("Public key class: {}", hostKeyVerifier.getRemoteHostKey().getPublicKey().getClass().getName()); // org.bouncycastle.jce.provider.JCERSAPublicKey implements RSAPublicKey
             log.debug("Public key algorithm: {}", hostKeyVerifier.getRemoteHostKey().getPublicKey().getAlgorithm());
             log.debug("Public key format: {}", hostKeyVerifier.getRemoteHostKey().getPublicKey().getFormat());
             log.debug("Public key bytes length: {}", hostKeyVerifier.getRemoteHostKey().getPublicKey().getEncoded().length);
             log.debug("Public key base64: {}", Base64.encodeBase64String(hostKeyVerifier.getRemoteHostKey().getPublicKey().getEncoded()));
             */

            RemoteHostKey remoteHostKey = hostKeyVerifier.getRemoteHostKey();
            if (remoteHostKey != null) {
                PublicKey publicKey = remoteHostKey.getPublicKey();
                if (publicKey != null && "X.509".equals(publicKey.getFormat()) && publicKey instanceof RSAPublicKey) {
                    RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
                    byte[] sshEncodedPublicKey = SshUtils.encodeSshRsaPublicKey((RSAPublicKey) rsaPublicKey);
                    target.setPublicKeyDigest(Digest.md5().digest(sshEncodedPublicKey).toHex());
                    response.setData(target);
                    /*
                     ByteArray modulus = new ByteArray(rsaPublicKey.getModulus());
                     ByteArray exponent = new ByteArray(rsaPublicKey.getPublicExponent());
                     log.debug("Public key modulus length: {}" , modulus.length());
                     log.debug("Public key exponent length: {}", exponent.length());
                     log.debug("Public key modulus md5: {}", Digest.md5().digest(modulus.getBytes()).toHex());
                     log.debug("Public key exponent md5: {}", Digest.md5().digest(exponent.getBytes()).toHex());
                     */


                    /*
                     try {
                     hostKeyStore.set(host, new Password(target.getPublicKeyDigest().toCharArray()));
                     hostKeyStore.close();
                     hostKeyStore = null;
                     } catch (IOException | KeyStoreException | InvalidKeySpecException e) {
                     log.error("Cannot store host key: {}", host, e);
                     }*/

                } else if (publicKey != null) {
                    log.error("Unsupported public key format '{}' from host: {}", publicKey.getFormat(), host);
                    response.getFaults().add(new Connection("Unsupported public key format"));
                } else {
                    log.error("No public key from remote host: {}", host);
                    response.getFaults().add(new Connection("No public key from remote host"));
                }
            }


        } else {

            // if password caching is enabled, look in our cache first.
            if (passwordStoreEnabled) {
                log.debug("password store is enabled, checking password");
                HashedPassword existing = getPassword(host);
                if (existing != null) {
                    if (isMatchingPassword(password, existing)) {
                        target.setPublicKeyDigest(getHostKey(host));
                        response.setData(target);
//                        response.setExtra(null);
                        return response;
                    }
                }
            }

            RemoteHostKeyDigestVerifier hostKeyVerifier = new RemoteHostKeyDigestVerifier("MD5", publicKeyDigest); // using MD5 because it's standard for ssh and output will match what user would see in terminal if they try to confirm with a command like ssh-keygen -l -f /etc/ssh/ssh_host_rsa_key.pub;  note that even recent versions of ssh-keygen don't support SHA-256, and without this support it would not be possible for user to confirm the hash is correct. if we switch to SHA-256 here and user cannot confirm it, user might just accept anything and it would be LESS secure than using md5 which the user can confirm.
            try (SSHClient ssh = new SSHClient()) {
                ssh.addHostKeyVerifier(hostKeyVerifier); // using our own verifier allows client to pass in the host key in sha256 hex format... for example 11aeeb41aaff0d206a8bddf93ba5d1255c97d1f14e21957fd0286e85d6ad161a ,  instead of the ssh format  22:95:2a:72:e2:41:94:f2:08:20:0e:76:fd:39:00:da 
                ssh.setConnectTimeout(timeout); // in seconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
                ssh.connect(host, port);
                ssh.authPassword(username, password);
                response.setData(target);

                if (hostKeyStoreEnabled) {
                    log.debug("host key store is enabled, storing host key");
                    storeHostKey(host, publicKeyDigest);
                }
                if (passwordStoreEnabled) {
                    log.debug("password store is enabled, storing password");
                    storePassword(host, password);
                }

                /*
                if( !loginRequest.getPackages().isEmpty() ) {
                    // TODO: check pre-reqs!
                    log.debug("Checking pre-requisites for packages: {}", StringUtils.join(loginRequest.packages, ", "));
                    checkPackages(ssh, loginRequest.getPackages(), response);
                }
                */
                
            } catch (IOException e) {
                log.error("Connection failed", e);
                response.getFaults().add(new Connection(e.getMessage())); // the Thrown fault from mtwilson-util-validation would show the entire stack trace to client; the custom Connection fault shows just the message
            }
        }

        // for a success response we already filled in the primary data
        // and we don't need to send the user input in extra
//        response.setExtra(null);

        return response;
    }
    
    public abstract static class HostPrecondition implements Condition, Faults {
        protected SSHClient ssh;
        protected String softwarePackageName;
        protected ArrayList<Fault> faults = new ArrayList<>();
        
        protected void fault(Fault fault) {
            faults.add(fault);
        }

        @Override
        public Collection<Fault> getFaults() {
            return faults;
        }
        
        
    }
    
    /*
    public static class OperatingSystem extends HostPrecondition {
        private HashSet<OperatingSystemInfo> allow = new HashSet<>();
        @Override
        public boolean test() {
            // send "lsb_release -a" command, get output
            // parse output for "Distributor ID"
            return false; // TODO
        }
        
    }
    */
    
    /*
    private void checkPackages(SSHClient ssh, List<String> packages, HostPrecheckResponse response) {
        HashSet<String> preconditions = new HashSet<>();
        if( packages.contains("attestation_service") ||packages.contains("director") ||packages.contains("key_broker")||packages.contains("key_broker")||packages.contains("key_broker_proxy")||packages.contains("openstack_extensions") ) {
            preconditions.add("ubuntu");
        }
        if( packages.contains("trustagent_ubuntu") ) {
            
        }
    }
    */
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class HostPrecheckRequest extends SSH {
        private List<String> packages = new ArrayList<>();

        public HostPrecheckRequest() {
            super();
        }

        public HostPrecheckRequest(String host, String password, String publicKeyDigest) {
            super(host, password, publicKeyDigest);
        }
        
        public List<String> getPackages() {
            return packages;
        }

        public void setPackages(List<String> packages) {
            this.packages = packages;
        }
        
        
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class HostPrecheckResponse implements Faults {

        private ArrayList<Fault> faults = new ArrayList<>();
        private SSH data;
//        private SSH extra;

        public HostPrecheckResponse() {
        }

        public SSH getData() {
            return data;
        }

        public void setData(SSH data) {
            this.data = data;
        }
/*
        public void setExtra(SSH extra) {
            this.extra = extra;
        }

        public SSH getExtra() {
            return extra;
        }*/

        @Override
        public Collection<Fault> getFaults() {
            return faults;
        }
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        
        hostKeyStoreEnabled = Boolean.valueOf(configuration.get("mtwilson.quickstart.ssh.hostkey.store", "false")).booleanValue();
        passwordStoreEnabled = Boolean.valueOf(configuration.get("mtwilson.quickstart.ssh.password.store", "false")).booleanValue();
        hostKeyFile = new File(Folders.repository("ssh") + File.separator + "ssh-host-keys.json"); // or .jck for the java keystore
        passwordFile = new File(Folders.repository("ssh") + File.separator + "ssh-passwords.json");  // or .jck for the java keystore
    }

    /**
     * private boolean hostKeyStoreEnabled, passwordStoreEnabled; private File
     * hostKeyFile, passwordFile; private PasswordKeyStore hostKeyStore,
     * passwordKeyStore; private Password hostKeyStorePassword,
     * passwordKeyStorePassword;
     *
     */
    private void loadConfiguration() {
        try {
            setConfiguration(ConfigurationFactory.getConfiguration());
        } catch (IOException e) {
            log.error("Cannot load configuration", e);
            setConfiguration(new PropertiesConfiguration());
        }
    }

    /**
     *
     * @param secret
     * @param alias
     * @return the existing password, a newly created password, or null if a
     * password is not available
     */
    private Password getPasswordFromVault(File secret, String alias) {
        try (PasswordKeyStore passwordVault = PasswordVaultFactory.getPasswordKeyStore(configuration)) {
            if (passwordVault.contains(alias)) {
                return passwordVault.get(alias);
            } else {
                log.error("Secret file exists but password is not in vault; deleting {}", secret.getAbsolutePath());
                secret.delete();
                Password newPassword = new Password(RandomUtil.randomHexString(16).replace("=", "").toCharArray());
                try {
                    passwordVault.set(alias, newPassword);
                    return newPassword;
                } catch (InvalidKeySpecException e) {
                    log.error("Cannot store password into vault: {}", alias, e);
                    return null;
                }
            }
        } catch (IOException | KeyStoreException e) {
            log.error("Cannot load password from vault: {}", alias, e);
            return null;
        }
    }

    private void loadHostKeyStore() {
        try {
            if (!hostKeyFile.getParentFile().exists()) {
                hostKeyFile.getParentFile().mkdirs();
            }
            if( hostKeyFile.exists() ) {
            ObjectMapper mapper = new ObjectMapper();
            hostKeys = mapper.readValue(hostKeyFile, HostKeyCollection.class);
            return;
            }
        } catch (IOException e) {
            log.error("Cannot read host keys: {}", hostKeyFile.getAbsolutePath(), e);
        }
            hostKeys = new HostKeyCollection();
        /*
         hostKeyStore = null;
         hostKeyStorePassword = getPasswordFromVault(hostKeyFile, "mtwilson.quickstart.ssh.password.store");
         if (hostKeyStorePassword != null) {
         try {
         hostKeyStore = new PasswordKeyStore("JCEKS", new FileResource(hostKeyFile), hostKeyStorePassword);
         } catch (IOException | NoSuchAlgorithmException | KeyStoreException e) {
         log.error("Cannot open host keys file", e);
         }
         }
         */
    }

    private void storeHostKey(String host, String hostKey) {
        try {
            if( hostKeys == null ) {
                loadHostKeyStore();
            }
            hostKeys.data.put(host, hostKey);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(hostKeyFile, hostKeys);
        } catch (IOException e) {
            log.error("Cannot write host keys: {}", hostKeyFile, e);
        }
    }

    private String getHostKey(String host) {
        if (hostKeys == null) {
            loadHostKeyStore();
        }
        return hostKeys.data.get(host);
    }

    private HashedPassword getPassword(String host) {
        if (passwords == null) {
            loadPasswordStore();
        }
        return passwords.data.get(host);
    }

    private boolean isMatchingHostKey(String host, String hostKey) {
        if (hostKeys == null) {
            loadHostKeyStore();
        }
        String existing = hostKeys.data.get(host);
        return existing.equalsIgnoreCase(hostKey);
    }

    private boolean isMatchingPassword(String inputPassword, HashedPassword existing) {
        try {
            HashedPassword input = new HashedPassword();
            input.setAlgorithm(existing.getAlgorithm());
            input.setIterations(existing.getIterations());
            input.setSalt(existing.getSalt());
            input.setPasswordHash(PasswordUtil.hash(inputPassword.getBytes("UTF-8"), input));
            return Arrays.equals(existing.getPasswordHash(), input.getPasswordHash());
        } catch (IOException e) {
            log.error("Cannot compare passwords", e);
            return false;
        }
    }

    private void storePassword(String host, String password) {
        try {
            if( passwords == null ) {
                loadPasswordStore();
            }
            HashedPassword hashed = new HashedPassword();
            hashed.setAlgorithm("SHA-256");
            hashed.setIterations(10000);
            hashed.setSalt(RandomUtil.randomByteArray(8));
            hashed.setPasswordHash(PasswordUtil.hash(password.getBytes("UTF-8"), hashed));
            passwords.data.put(host, hashed);
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(passwordFile, passwords);
        } catch (IOException e) {
            log.error("Cannot write passwords: {}", passwordFile, e);
        }
    }

    private void loadPasswordStore() {
        try {
            if (!passwordFile.getParentFile().exists()) {
                passwordFile.getParentFile().mkdirs();
            }
            if( passwordFile.exists()  ) {
            ObjectMapper mapper = new ObjectMapper();
            passwords = mapper.readValue(passwordFile, HashedPasswordCollection.class);
            return;
            }
        } catch (IOException e) {
            log.error("Cannot read host keys: {}", passwordFile.getAbsolutePath(), e);
        }
            passwords = new HashedPasswordCollection();
        /*
         passwordStore = null;
         passwordStorePassword = getPasswordFromVault(passwordFile, "mtwilson.quickstart.ssh.password.store");
         if (passwordStorePassword != null) {
         try {
         passwordStore = new PasswordKeyStore("JCEKS", new FileResource(passwordFile), passwordStorePassword);
         } catch (IOException | NoSuchAlgorithmException | KeyStoreException e) {
         log.error("Cannot open password file", e);
         }
         }
         */
    }

    public static class HostKeyCollection {

        public HashMap<String, String> data = new HashMap<>();
    }

    public static class HashedPasswordCollection {

        public HashMap<String, HashedPassword> data = new HashMap<>();
    }
}
