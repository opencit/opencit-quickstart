/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task.jaxrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.mtwilson.jaxrs2.DocumentCollection;
import com.intel.mtwilson.task.TaskDocument;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="task_collection")
public class TaskDocumentCollection extends DocumentCollection<TaskDocument> {
    private final ArrayList<TaskDocument> tasks = new ArrayList<>();
    
    // using the xml annotations we get output like <hosts><host>...</host><host>...</host></hosts> , without them we would have <hosts><hosts>...</hosts><hosts>...</hosts></hosts> and it looks strange
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS) // jackson 1.9
    @JsonInclude(JsonInclude.Include.ALWAYS)                // jackson 2.0
    @JacksonXmlElementWrapper(localName="tasks")
    @JacksonXmlProperty(localName="task")    
    public List<TaskDocument> getTasks() { return tasks; }

    @Override
    public List<TaskDocument> getDocuments() {
        return getTasks();
    }
    
}