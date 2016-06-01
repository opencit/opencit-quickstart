/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.Locator;
import javax.ws.rs.PathParam;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackageLocator implements Locator<SoftwarePackageDocument> {

    @PathParam("id")
    public UUID id;

    @Override
    public void copyTo(SoftwarePackageDocument item) {
        if( id != null ) {
            item.setId(id);
        }
    }

}
