/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.intel.mtwilson.deployment.FaultDescriptor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.dcsg.cpg.validation.Faults;
import com.intel.mtwilson.deployment.descriptor.NetworkRole;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.jaxrs2.Document;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Faults may be added by the server; a client should not send a document with
 * faults.
 *
 * @author jbuhacoff
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(localName = "order")
public class OrderDocument extends Document implements Faults {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDocument.class);
    private final List<Fault> faults = new ArrayList<>();
    private final HashSet<FaultDescriptor> faultDescriptors = new HashSet<>();
    private Set<String> features;
    private Set<Target> targets;
    private NetworkRole networkRole;
    
    /**
     * The "settings" map is initialized with user input settings and is updated
     * by various tasks as they generate settings for different software
     * packages. All settings share the same global namespace, and for that
     * reason each setting name should be prefixed with some context about which
     * service or component uses it, to avoid collisions. Therefore, even if
     * multiple services each support a setting of the same name like
     * "jetty.port", when stored in this settings object those settings should
     * be prefixed by the web service name, like "trustagent.jetty.port" and
     * "director.jetty.port"
     */
    private Map<String, String> settings = new HashMap<>();
    private String status = "PENDING"; // PENDING, ACTIVE, DONE, CANCELLED
    private Long progress, progressMax;
    private HashMap<String, TaskDocument> taskMap = new HashMap<>();

    @JsonIgnore
    @Override
    public Collection<Fault> getFaults() {
        return faults;
    }

    @JsonProperty("faults")
    public Collection<FaultDescriptor> getFaultDescriptors() {
        for (Fault fault : faults) {
            faultDescriptors.add(new FaultDescriptor(fault.getClass().getName(), fault.getDescription()));
        }
        return faultDescriptors;
    }

    @JsonProperty("faults")
    public void setFaultDescriptors(Collection<FaultDescriptor> list) {
        faultDescriptors.addAll(list);
    }

    public Set<String> getFeatures() {
        return features;
    }

    public Set<Target> getTargets() {
        return targets;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public String getStatus() {
        return status;
    }

    public void setFeatures(Set<String> features) {
        this.features = features;
    }

    public void setTargets(Set<Target> targets) {
        this.targets = targets;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getProgress() {
        return progress;
    }

    public Long getProgressMax() {
        return progressMax;
    }

    public void setProgress(Long progress) {
        this.progress = progress;
    }

    public void setProgressMax(Long progressMax) {
        this.progressMax = progressMax;
    }

    public NetworkRole getNetworkRole() {
        return networkRole;
    }

    public void setNetworkRole(NetworkRole networkRole) {
        this.networkRole = networkRole;
    }
    
    

    /**
     * NOTE: the result collection is NOT modifiable!
     */
    public Collection<TaskDocument> getTasks() {
        return taskMap.values();
    }

    public void setTasks(Collection<TaskDocument> tasks) {
        taskMap.clear();
        for (TaskDocument task : tasks) {
            taskMap.put(task.getId().toString(), task);
        }
    }

    @JsonIgnore
    public Map<String, TaskDocument> getTaskMap() {
        return taskMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isInstance(obj)) {
            return false;
        }
        OrderDocument other = (OrderDocument) obj;
        Set<String> otherFeatures = other.getFeatures();
        Set<Target> otherTargets = other.getTargets();
        Map<String, String> otherSettings = other.getSettings();
        NetworkRole otherNetworkRole = other.getNetworkRole();
        boolean equalFeatures = false;
        boolean equalTargets = false;
        boolean equalSettings = false;
        boolean equalNetworkRole = false;
        if (features == null && otherFeatures == null || features != null && otherFeatures != null && features.equals(otherFeatures)) {
            equalFeatures = true;
        }
        if (targets == null && otherTargets == null || targets != null && otherTargets != null && targets.equals(otherTargets)) {
            equalTargets = true;
        }
        if (settings == null && otherSettings == null || settings != null && otherSettings != null && settings.equals(otherSettings)) {
            equalSettings = true;
        }
        if (networkRole == null && otherNetworkRole == null || networkRole != null && otherNetworkRole != null && networkRole.equals(otherNetworkRole)) {
            equalNetworkRole = true;
        }
        log.debug("equalFeatures? {}", equalFeatures);
        log.debug("equalTargets? {}", equalTargets);
        log.debug("equalSettings? {}", equalSettings);
        log.debug("equalNetworkRole? {}", equalNetworkRole);
        return equalFeatures && equalTargets && equalSettings && equalNetworkRole;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.features);
        hash = 37 * hash + Objects.hashCode(this.targets);
        hash = 37 * hash + Objects.hashCode(this.settings);
        return hash;
    }
}
