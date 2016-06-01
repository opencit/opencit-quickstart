/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.mtwilson.jaxrs2.Document;
import java.util.Set;

/**
 * Faults may be added by the server; a client should not send a document with
 * faults.
 *
 * @author jbuhacoff
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(localName = "package")
public class SoftwarePackageDocument extends Document {
    private String packageName;
    private Set<String> availableVariants;

    public void setName(String packageName) {
        this.packageName = packageName;
    }

    public String getName() {
        return packageName;
    }

    public void setAvailableVariants(Set<String> availableVariants) {
        this.availableVariants = availableVariants;
    }

    public Set<String> getAvailableVariants() {
        return availableVariants;
    }
    
}
