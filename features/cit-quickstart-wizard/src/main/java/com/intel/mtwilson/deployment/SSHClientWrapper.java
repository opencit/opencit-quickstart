/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.util.ssh.RemoteHostKeyDigestVerifier;
import java.io.Closeable;
import java.io.IOException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;

/**
 *
 * @author jbuhacoff
 */
public class SSHClientWrapper implements Closeable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SSHClientWrapper.class);
    private final SSH remote;
    private SSHClient client;

    public SSHClientWrapper(SSH remote) {
        this.remote = remote;
    }

    public SSH getRemote() {
        return remote;
    }
    
    public void connect() throws IOException {
        if( client != null && client.isConnected() ) {
            throw new IllegalStateException("Already connected to "+remote.getHost());
        }
        RemoteHostKeyDigestVerifier hostKeyVerifier = new RemoteHostKeyDigestVerifier("MD5", remote.getPublicKeyDigest()); // md5 is insecure but this is done for compatibility with command-line ssh clients that show md5 hash of remote host public key for user to verify
        client = new SSHClient();
        client.addHostKeyVerifier(hostKeyVerifier); // this accepts all remote public keys, then you have to verify the remote host key before continuing!!!
        client.setTimeout(remote.getTimeout());
        client.connect(remote.getHost(), remote.getPort());
        client.authPassword(remote.getUsername(), remote.getPassword());
    }
    
    public Session session() throws ConnectionException, TransportException {
        return client.startSession();
    }
    
    public SFTPClient sftp() throws IOException {
        return client.newSFTPClient();
    }
    
    public void disconnect() throws IOException {
        client.disconnect();
    }

    @Override
    public void close() throws IOException {
        if( client != null && client.isConnected() ) {
            disconnect();
        }
    }
    

    
}
