/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.dcsg.cpg.io.PropertiesUtil;
import com.intel.mtwilson.core.junit.Env;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.task.AbstractRemoteTask;
import com.intel.mtwilson.util.exec.Result;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDigestVerifier;
import com.intel.mtwilson.util.validation.faults.Thrown;
import java.io.IOException;
import java.util.Properties;
import net.schmizz.sshj.SSHClient;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class RemoteTaskTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RemoteTaskTest.class);
    private static SSH remote;

    @BeforeClass
    public static void init() throws IOException {
        Properties properties = PropertiesUtil.removePrefix(Env.getProperties("cit3-test-ssh"), "cit3.test.ssh.");
        remote = new SSH(properties.getProperty("host"), properties.getProperty("password"), properties.getProperty("publicKeyDigest"));
    }

    @Test
    public void testRemoteTask() {
        DirectoryListing ls = new DirectoryListing(remote);
        ls.run();
    }
    
    @Test
    public void testManyTimes() {
        for(int i=0; i<100; i++) {
            DirectoryListing ls = new DirectoryListing(remote);
            ls.run();
            if( !ls.getFaults().isEmpty() ) {
                log.debug("stopped at iteration {}", i);
                break;
            }
        }
    }

    public static class DirectoryListing extends AbstractRemoteTask {

        private SSH remote;

        public DirectoryListing(SSH remote) {
            super();
            this.remote = remote;
        }

        @Override
        public void execute() {
            log.debug("task id: {}", getId());
//            RemoteHostKeyDigestVerifier hostKeyVerifier = new RemoteHostKeyDigestVerifier("MD5", remote.getPublicKeyDigest()); // using MD5 because it's standard for ssh and output will match what user would see in terminal if they try to confirm with a command like ssh-keygen -l -f /etc/ssh/ssh_host_rsa_key.pub;  note that even recent versions of ssh-keygen don't support SHA-256, and without this support it would not be possible for user to confirm the hash is correct. if we switch to SHA-256 here and user cannot confirm it, user might just accept anything and it would be LESS secure than using md5 which the user can confirm.
            //try (SSHClient ssh = new SSHClient()) {
            try(SSHClientWrapper ssh = new SSHClientWrapper(remote)) {
                /*
                ssh.addHostKeyVerifier(hostKeyVerifier); // using our own verifier allows client to pass in the host key in sha256 hex format... for example 11aeeb41aaff0d206a8bddf93ba5d1255c97d1f14e21957fd0286e85d6ad161a ,  instead of the ssh format  22:95:2a:72:e2:41:94:f2:08:20:0e:76:fd:39:00:da 
                ssh.setConnectTimeout(2000); // in milliseconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
                ssh.connect(remote.getHost(), remote.getPort()); // may throw java.net.SocketTimeoutException: connect timed out
                ssh.authPassword(remote.getUsername(), remote.getPassword());
*/
                Result result = sshexec(ssh, "/usr/bin/sha1sum /opt/mtwilson/configuration/ssl.crt | /usr/bin/awk '{print $1}'");
//                Result result = sshexec(ssh, "/bin/ls");
//                Result result = sshexec(ssh, "/bin/bash monitor.sh mtwilson-server-3.0-SNAPSHOT-jdk_glassfish_monit.bin mtwilson-server-3.0-SNAPSHOT-jdk_glassfish_monit.bin.mark /tmp/cit/monitor/xyz >/dev/null &");
                log.debug("result: {}", result.getStdout());
            } catch (Exception e) {
                fault(new Thrown(e));
                log.error("Cannot connect", e);
            }
        }
    }
}
