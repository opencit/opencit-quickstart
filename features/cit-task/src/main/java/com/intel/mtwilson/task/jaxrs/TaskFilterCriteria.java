/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task.jaxrs;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.DefaultFilterCriteria;
import com.intel.mtwilson.repository.FilterCriteria;
import com.intel.mtwilson.task.TaskDocument;
import javax.ws.rs.QueryParam;

/**
 *
 * @author jbuhacoff
 */
public class TaskFilterCriteria extends DefaultFilterCriteria implements FilterCriteria<TaskDocument> {
    @QueryParam("id")
    public UUID id;
    @QueryParam("nameEqualTo")
    public String nameEqualTo;
    @QueryParam("nameContains")
    public String nameContains;
    @QueryParam("descriptionContains")
    public String descriptionContains;
    
}
