/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task.jaxrs;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.DocumentCollection;
import com.intel.mtwilson.jaxrs2.NoLinks;
import com.intel.mtwilson.jaxrs2.Patch;
import com.intel.mtwilson.jaxrs2.server.resource.AbstractJsonapiResource;
import com.intel.mtwilson.jaxrs2.server.resource.DocumentRepository;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.task.Action;
import com.intel.mtwilson.task.Status;
import com.intel.mtwilson.task.TaskDocument;
import com.intel.mtwilson.task.TaskDocumentRepository;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/tasks")
public class Tasks extends AbstractJsonapiResource<TaskDocument, TaskDocumentCollection, TaskFilterCriteria, NoLinks<TaskDocument>, TaskLocator> {

    private TaskDocumentRepository repository;
    
    public Tasks() {
        repository = new TaskDocumentRepository();
    }
    
    @Override
    protected TaskDocumentCollection createEmptyCollection() {
        return new TaskDocumentCollection();
    }

    @Override
    protected TaskDocumentRepository getRepository() {
        return repository;
    }

    /*
    private String getStatusLink(String taskId) {
        return "/v1/tasks/"+taskId;
    }
    private String getCancelLink(String taskId) {
        return "/v1/tasks/"+taskId+"/cancel";
    }*/
    
    @POST
    @Path("/{taskId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public TaskDocument cancel(@PathParam("taskId") String taskId) {
        TaskLocator locator = new TaskLocator();
        locator.id = UUID.valueOf(taskId);
        TaskDocument task = repository.retrieve(locator);
        if (task != null) {
            task.setStatus(Status.CANCELLED);
            for(Action action : task.getActions() ) {
                action.setStatus(Status.CANCELLED);
            }
        }
        return task;
    }
    
    
    // refuse to let client store or patch tasks arbitrarily;  server controls updates to tasks
    
    @Override
    public TaskDocumentCollection storeJsonapiCollection(TaskLocator locator, TaskDocumentCollection collection) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public TaskDocument storeOne(TaskLocator locator, TaskDocument item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskDocumentCollection patchJsonapiCollection(TaskLocator locator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TaskDocument patchOne(TaskLocator locator, Patch<TaskDocument, TaskFilterCriteria, NoLinks<TaskDocument>>[] patchArray) {
        throw new UnsupportedOperationException();
    }

    

}
