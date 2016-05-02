/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.mtwilson.jaxrs2.Document;

/**
 * Faults may be added by the server; a client should not send a document with
 * faults.
 *
 * @author jbuhacoff
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(localName = "package")
public class SoftwarePackageDocument extends Document {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SoftwarePackageDocument.class);

    private String packageName;
    private Boolean available;

    public void setName(String packageName) {
        this.packageName = packageName;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
    
    public String getName() {
        return packageName;
    }

    public Boolean getAvailable() {
        return available;
    }
    
}
