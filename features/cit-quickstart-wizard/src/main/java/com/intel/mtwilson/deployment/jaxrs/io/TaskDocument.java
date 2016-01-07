/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.dcsg.cpg.validation.Faults;
import com.intel.mtwilson.deployment.FaultDescriptor;
import com.intel.mtwilson.jaxrs2.Document;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jbuhacoff
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(localName="task")
public class TaskDocument extends Document implements Faults {
    private final List<Fault> faults = new ArrayList<>();
    private final HashSet<FaultDescriptor> faultDescriptors = new HashSet<>();
    private String name;
    // the sequence number of the task;  this is optional but is useful when need to convey an order of tasks to client when they may be out of order in the response
    private int sequence = 0;
    private boolean done;
    private long progress, progressMax;
    private Map<String,Object> data;

    public void setName(String name) {
        this.name = name;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
    
    public void setProgress(long progress) {
        this.progress = progress;
    }

    public void setProgressMax(long progressMax) {
        this.progressMax = progressMax;
    }

    public String getName() {
        return name;
    }

    public boolean isDone() {
        return done;
    }

    public long getProgress() {
        return progress;
    }

    public long getProgressMax() {
        return progressMax;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getSequence() {
        return sequence;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    @JsonIgnore
    @Override
    public Collection<Fault> getFaults() {
        return faults;
    }
    
    @JsonProperty("faults")
    public Collection<FaultDescriptor> getFaultDescriptors() {
        for(Fault fault : faults) {
            faultDescriptors.add(new FaultDescriptor(fault.getClass().getName(), fault.getDescription()));
        }
        return faultDescriptors;
    }
    
    @JsonProperty("faults")
    public void setFaultDescriptors(List<FaultDescriptor> list) {
        faultDescriptors.addAll(list);
    }
    
}
