/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.dcsg.cpg.validation.Faults;
import com.intel.mtwilson.jaxrs2.Document;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="task")
public class TaskDocument extends Document implements Faults {
    
    private String label;
    private List<Action> actions = new ArrayList<>();
    private Integer current;
    private Integer max;
    private Status status;
    private final List<Fault> faults = new ArrayList<>();

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public Collection<Fault> getFaults() {
        return faults;
    }

    
}
