/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task.jaxrs;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.Locator;
import com.intel.mtwilson.task.TaskDocument;
import javax.ws.rs.PathParam;

/**
 *
 * @author jbuhacoff
 */
public class TaskLocator implements Locator<TaskDocument> {

    @PathParam("id")
    public UUID id;

    @Override
    public void copyTo(TaskDocument item) {
        if( id != null ) {
            item.setId(id);
        }
    }

}
