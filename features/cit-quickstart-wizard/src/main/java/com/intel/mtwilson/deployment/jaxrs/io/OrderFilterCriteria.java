/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.DefaultFilterCriteria;
import com.intel.mtwilson.repository.FilterCriteria;
import javax.ws.rs.QueryParam;

/**
 *
 * @author jbuhacoff
 */
public class OrderFilterCriteria extends DefaultFilterCriteria implements FilterCriteria<OrderDocument> {
    @QueryParam("id")
    public UUID id;
    @QueryParam("nameEqualTo")
    public String nameEqualTo;
    @QueryParam("nameContains")
    public String nameContains;
    @QueryParam("descriptionContains")
    public String descriptionContains;
    
}
