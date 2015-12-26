/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.intel.mtwilson.deployment.jaxrs.faults.Null;
import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.io.ByteArray;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.dcsg.cpg.validation.Faults;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.deployment.jaxrs.faults.ConnectionTimeout;
import com.intel.mtwilson.deployment.jaxrs.faults.NoRouteToHost;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.util.ssh.RemoteHostKey;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDeferredVerifier;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDigestVerifier;
import com.intel.mtwilson.util.ssh.SshUtils;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import org.apache.commons.codec.binary.Base64;

/**
 * Call without public key and password to just get the remote host's public key
 * and return it
 *
 * Call with public key and password to attempt login to remote host and report
 * success or failure
 *
 * @author jbuhacoff
 */
@V2
@Path("/rpc/ssh-login")
public class SshLogin {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SshLogin.class);

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
     * @param loginRequest
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SshLoginResponse deploy(SSH loginRequest) {
        SshLoginResponse response = new SshLoginResponse();
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
        if( timeout == null ) {
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
        response.setExtra(target);

        if (!response.getFaults().isEmpty()) {
            return response;
        }


        // confirm it, then create a new SSH object without the password to return
        if (publicKeyDigest == null) {
            RemoteHostKeyDeferredVerifier hostKeyVerifier = new RemoteHostKeyDeferredVerifier();
            try (SSHClient ssh = new SSHClient()) {
                ssh.addHostKeyVerifier(hostKeyVerifier); // this accepts all remote public keys, then you have to verify the remote host key before continuing!!!
                ssh.setConnectTimeout(timeout); // in milliseconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
                ssh.connect(host, port);
                ssh.authPassword(username, ""); // we don't actually send the password when we're just trying to get the remote host public key
                // shouldn't get here because we expect authentication to fail
                log.warn("Login successful to remote server without a password");
                // pass through to getRemoteHostKey() below
            } catch(SocketTimeoutException e) {
                log.debug("Connection to {} timeout after {}ms: {}", host, timeout, e.getMessage());
                response.getFaults().add(new ConnectionTimeout(host, timeout));
                return response;
            } catch(ConnectException e) {
                log.debug("Connection failed with ConnectException: {}", e.getMessage());
                String message = e.getMessage();
                if( message != null && message.contains("Connection timed out")) {
                    response.getFaults().add(new ConnectionTimeout(host, timeout));
                }
                else {
                    response.getFaults().add(new Connection(host));
                }
                return response;
            } catch(NoRouteToHostException e) {
                log.debug("Connection failed with NoRouteToHostException: {}", e.getMessage());
                response.getFaults().add(new NoRouteToHost(host));
                return response;
            } catch(TransportException e) {
                log.debug("Connection failed with TransportException: {}", e.getMessage());
                response.getFaults().add(new Connection(host));
                return response;
            } catch(UserAuthException e) {
                // this exception is expected because we provided an empty password
                log.debug("Caught expected UserAuthException: {}", e.getMessage());
                // pass through to getRemoteHostKey() below
            } catch(IOException e) {
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
                } else if( publicKey != null ) {
                    log.error("Unsupported public key format '{}' from host: {}", publicKey.getFormat(), host);
                    response.getFaults().add(new Connection("Unsupported public key format"));
                }
                else {
                    log.error("No public key from remote host: {}", host);
                    response.getFaults().add(new Connection("No public key from remote host"));
                }
            }


        } else {
            RemoteHostKeyDigestVerifier hostKeyVerifier = new RemoteHostKeyDigestVerifier("MD5", publicKeyDigest); // using MD5 because it's standard for ssh and output will match what user would see in terminal if they try to confirm with a command like ssh-keygen -l -f /etc/ssh/ssh_host_rsa_key.pub;  note that even recent versions of ssh-keygen don't support SHA-256, and without this support it would not be possible for user to confirm the hash is correct. if we switch to SHA-256 here and user cannot confirm it, user might just accept anything and it would be LESS secure than using md5 which the user can confirm.
            try (SSHClient ssh = new SSHClient()) {
                ssh.addHostKeyVerifier(hostKeyVerifier); // using our own verifier allows client to pass in the host key in sha256 hex format... for example 11aeeb41aaff0d206a8bddf93ba5d1255c97d1f14e21957fd0286e85d6ad161a ,  instead of the ssh format  22:95:2a:72:e2:41:94:f2:08:20:0e:76:fd:39:00:da 
                ssh.setConnectTimeout(timeout); // in seconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
                ssh.connect(host, port);
                ssh.authPassword(username, password);
                response.setData(target);
            } catch (IOException e) {
                log.error("Connection failed", e);
                response.getFaults().add(new Connection(e.getMessage())); // the Thrown fault from mtwilson-util-validation would show the entire stack trace to client; the custom Connection fault shows just the message
            }
        }
        
        // for a success response we already filled in the primary data
        // and we don't need to send the user input in extra
        response.setExtra(null);

        return response;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class SshLoginResponse implements Faults {

        private ArrayList<Fault> faults = new ArrayList<>();
        private SSH data;
        private SSH extra;

        public SshLoginResponse() {
        }

        public SSH getData() {
            return data;
        }

        public void setData(SSH data) {
            this.data = data;
        }

        public void setExtra(SSH extra) {
            this.extra = extra;
        }

        public SSH getExtra() {
            return extra;
        }
        

        @Override
        public Collection<Fault> getFaults() {
            return faults;
        }
    }
}
