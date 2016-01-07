/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.task.AbstractRemoteTask;
import com.intel.mtwilson.util.exec.Result;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDigestVerifier;
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
    public static void init() {
        remote = new SSH("10.1.68.34", "P@ssw0rd", "22952a72e24194f208200e76fd3900da");
    }

    @Test
    public void testRemoteTask() {
        DirectoryListing ls = new DirectoryListing(remote);
        ls.run();
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
            RemoteHostKeyDigestVerifier hostKeyVerifier = new RemoteHostKeyDigestVerifier("MD5", remote.getPublicKeyDigest()); // using MD5 because it's standard for ssh and output will match what user would see in terminal if they try to confirm with a command like ssh-keygen -l -f /etc/ssh/ssh_host_rsa_key.pub;  note that even recent versions of ssh-keygen don't support SHA-256, and without this support it would not be possible for user to confirm the hash is correct. if we switch to SHA-256 here and user cannot confirm it, user might just accept anything and it would be LESS secure than using md5 which the user can confirm.
            try (SSHClient ssh = new SSHClient()) {
                ssh.addHostKeyVerifier(hostKeyVerifier); // using our own verifier allows client to pass in the host key in sha256 hex format... for example 11aeeb41aaff0d206a8bddf93ba5d1255c97d1f14e21957fd0286e85d6ad161a ,  instead of the ssh format  22:95:2a:72:e2:41:94:f2:08:20:0e:76:fd:39:00:da 
                ssh.setConnectTimeout(600); // in seconds; connection attempt will be cancelled if it takes longer than this amount of time to connect
                ssh.connect(remote.getHost(), remote.getPort());
                ssh.authPassword(remote.getUsername(), remote.getPassword());

//                Result result = sshexec(ssh, "/bin/ls");
                Result result = sshexec(ssh, "/bin/bash monitor.sh mtwilson-server-3.0-SNAPSHOT-jdk_glassfish_monit.bin mtwilson-server-3.0-SNAPSHOT-jdk_glassfish_monit.bin.mark /tmp/cit/monitor/xyz >/dev/null &");
                log.debug("result: {}", result.getStdout());
            } catch (Exception e) {
                log.error("Cannot connect", e);
            }
        }
    }
}
