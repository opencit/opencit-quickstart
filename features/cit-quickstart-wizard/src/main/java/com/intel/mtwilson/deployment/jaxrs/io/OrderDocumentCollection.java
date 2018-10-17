/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.mtwilson.jaxrs2.DocumentCollection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="order_collection")
public class OrderDocumentCollection extends DocumentCollection<OrderDocument> {
    private final ArrayList<OrderDocument> orders = new ArrayList<>();
    
    // using the xml annotations we get output like <hosts><host>...</host><host>...</host></hosts> , without them we would have <hosts><hosts>...</hosts><hosts>...</hosts></hosts> and it looks strange
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS) // jackson 1.9
    @JsonInclude(JsonInclude.Include.ALWAYS)                // jackson 2.0
    @JacksonXmlElementWrapper(localName="orders")
    @JacksonXmlProperty(localName="order")    
    public List<OrderDocument> getOrders() { return orders; }

    @Override
    public List<OrderDocument> getDocuments() {
        return getOrders();
    }
    
}