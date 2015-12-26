/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

import java.util.Objects;

/**
 *
 * @author jbuhacoff
 */
public class SSH {
    private String host;
    private Integer port = 22;
    private String username = "root";
    private String password;
    private String publicKeyDigest;
    private Integer timeout = 15000; // milliseconds

    public SSH() {
    }

    public SSH(String host, String password, String publicKeyDigest) {
        this.host = host;
        this.password = password;
        this.publicKeyDigest = publicKeyDigest;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    
    public String getPassword() {
        return password;
    }

    public String getPublicKeyDigest() {
        return publicKeyDigest;
    }

    public Integer getTimeout() {
        return timeout;
    }


    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    
    public void setPassword(String password) {
        this.password = password;
    }

    public void setPublicKeyDigest(String publicKeyDigest) {
        this.publicKeyDigest = publicKeyDigest;
    }

    /**
     * Sets the connection timeout; the connection attempt will be cancelled if
     * the remote host does not respond within this amount of time
     * 
     * @param timeout in seconds
     */
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    
    
    /**
     * Note that the timeout value is NOT considered when checking equality.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isInstance(obj)) {
            return false;
        }
        SSH other = (SSH) obj;
        String otherHost = other.getHost();
        Integer otherPort = other.getPort();
        String otherUsername = other.getUsername();
        String otherPassword = other.getPassword();
        String otherPublicKeyDigest = other.getPublicKeyDigest();
        Integer otherTimeout = other.getTimeout();
        boolean equalHost = false;
        boolean equalPort = false;
        boolean equalUsername = false;
        boolean equalPassword = false;
        boolean equalPublicKeyDigest = false;
        boolean equalTimeout = false;
        if (host == null && otherHost == null || host != null && otherHost != null && host.equals(otherHost)) {
            equalHost = true;
        }
        if (port == null && otherPort == null || port != null && otherPort != null && port.equals(otherPort)) {
            equalPort = true;
        }
        if (username == null && otherUsername == null || username != null && otherUsername != null && username.equals(otherUsername)) {
            equalUsername = true;
        }
        if (password == null && otherPassword == null || password != null && otherPassword != null && password.equals(otherPassword)) {
            equalPassword = true;
        }
        if (publicKeyDigest == null && otherPublicKeyDigest == null || publicKeyDigest != null && otherPublicKeyDigest != null && publicKeyDigest.equals(otherPublicKeyDigest)) {
            equalPublicKeyDigest = true;
        }
        if (timeout == null && otherTimeout == null || timeout != null && otherTimeout != null && timeout.equals(otherTimeout)) {
            equalTimeout = true;
        }
        return equalHost && equalPort && equalUsername && equalPassword && equalPublicKeyDigest && equalTimeout;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.host);
        hash = 31 * hash + Objects.hashCode(this.port);
        hash = 31 * hash + Objects.hashCode(this.username);
        hash = 31 * hash + Objects.hashCode(this.password);
        hash = 31 * hash + Objects.hashCode(this.publicKeyDigest);
        hash = 31 * hash + Objects.hashCode(this.timeout);
        return hash;
    }

}
