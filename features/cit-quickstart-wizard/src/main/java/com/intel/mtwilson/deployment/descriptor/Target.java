/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author jbuhacoff
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Target extends SSH {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Target.class);

    /**
     * The unique names of packages to install on this target host
     */
    private Set<String> packages;
    
    /**
     * The network role of this target host may affect its configuration
     */
    private NetworkRole networkRole;
    
    /**
     * The unique names of packages that are installed on this target host
     * (the client does not set this - the server updates this set during
     * the installation)
     */
    private Set<String> packagesInstalled;

    public Target() {
    }

    
    public Set<String> getPackages() {
        return packages;
    }

    public NetworkRole getNetworkRole() {
        return networkRole;
    }

    public Set<String> getPackagesInstalled() {
        return packagesInstalled;
    }
    
    

    public void setPackages(Set<String> packages) {
        this.packages = packages;
    }

    public void setNetworkRole(NetworkRole networkRole) {
        this.networkRole = networkRole;
    }

    public void setPackagesInstalled(Set<String> packagesInstalled) {
        this.packagesInstalled = packagesInstalled;
    }

    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isInstance(obj)) {
            return false;
        }
        Target other = (Target) obj;
        Set<String> otherPackages = other.getPackages();
        NetworkRole otherNetworkRole = other.getNetworkRole();
        boolean equalPackages = false;
        boolean equalNetworkRole = false;
        if (packages == null && otherPackages == null || packages != null && otherPackages != null && packages.equals(otherPackages)) {
            equalPackages = true;
        }
        if (networkRole == null && otherNetworkRole == null || networkRole != null && otherNetworkRole != null && networkRole.equals(otherNetworkRole)) {
            equalNetworkRole = true;
        }
        return equalPackages && equalNetworkRole && super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 57 * hash + super.hashCode();
        hash = 57 * hash + Objects.hashCode(this.packages);
        hash = 57 * hash + Objects.hashCode(this.networkRole);
        return hash;
    }
}
