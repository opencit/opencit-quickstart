/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.descriptor;

/**
 *
 * @author jbuhacoff
 */
public enum NetworkRole {
    /**
     * Private data center (private cloud); all components installed in same network
     */
    PRIVATE,
    /**
     * Public data center (public cloud provider); attestation, director, and key broker proxy may be installed
     */
    PROVIDER, 
    /**
     * Private data center (public cloud enterprise customer); director and key broker may be installed
     */
    SUBSCRIBER;
}
