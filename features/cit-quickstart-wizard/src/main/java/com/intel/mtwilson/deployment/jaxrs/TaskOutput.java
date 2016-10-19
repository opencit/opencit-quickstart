/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.jaxrs2.Link;
import com.intel.mtwilson.launcher.ws.ext.V2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/quickstart/tasks")
public class TaskOutput {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskOutput.class);
    
    @GET
    @Path("{taskId}/output")
    @Produces(MediaType.APPLICATION_JSON)
    public DirectoryListing getOutputForTask(@PathParam("taskId") String taskId) {

        DirectoryListing result = new DirectoryListing();

        // TODO:  need to refactor part of mtwilson-core-html5 directory so we can use it here too
        String outputPath = Folders.repository("tasks") + File.separator + taskId;
        File outputDirectory = new File(outputPath);

        File[] files = outputDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    result.links.add(new Link("file", "/v1/quickstart/tasks/"+taskId+"/output/"+file.getName()));
                }
            }
        }
        return result;
    }
    
    
    @GET
    @Path("{taskId}/output/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getOutputFileForTask(@PathParam("taskId") String taskId, @PathParam("filename") String filename) {
        String path = Folders.repository("tasks") + File.separator + taskId + File.separator + filename;
        File file = new File(path);
        if( !file.getAbsolutePath().startsWith(Folders.repository("tasks")+File.separator) || !file.exists() || !file.canRead() || !file.isFile() ) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        try {
            return FileUtils.readFileToString(file, "UTF-8");
        }
        catch(IOException e) {
            log.error("Cannot read file: {}", path, e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    public static class DirectoryListing {
        public ArrayList<Link> links = new ArrayList<>();
    }
}
