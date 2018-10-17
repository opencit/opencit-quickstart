/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.Locator;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 *
 * @author jbuhacoff
 */
public class OrderLocator implements Locator<OrderDocument> {

    @PathParam("id")
    public UUID id;
    
    @QueryParam("token")
    public String token;

    @Override
    public void copyTo(OrderDocument item) {
        if( id != null ) {
            item.setId(id);
        }
    }

}
