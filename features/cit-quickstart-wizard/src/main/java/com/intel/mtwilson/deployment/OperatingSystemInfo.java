/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

/**
 * Example Ubuntu:
 *
 * <pre>
Distributor ID: Ubuntu
Description:    Ubuntu 12.04.5 LTS
Release:        12.04
Codename:       precise
 * </pre>
 *
 * Example RedHat:
 *
 * <pre>
Distributor ID: RedHatEnterpriseServer
Description:    Red Hat Enterprise Linux Server release 7.0 (Maipo)
Release:        7.0
Codename:       Maipo
 * </pre>
 */
public class OperatingSystemInfo {
    private String distributor;
    private String version;

    public OperatingSystemInfo(String distributor, String version) {
        this.distributor = distributor;
        this.version = version;
    }

    public String getDistributor() {
        return distributor;
    }

    public String getVersion() {
        return version;
    }
    
    public String getDistributorName() {
        if(distributor.contains("Ubuntu"))
                return "ubuntu";
        else if(distributor.contains("RedHat"))
            return "redhat";
        else
            return null;
    }
    
}
