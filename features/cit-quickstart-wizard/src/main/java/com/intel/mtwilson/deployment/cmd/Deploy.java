/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.console.AbstractCommand;
import com.intel.dcsg.cpg.console.Command;
import com.intel.mtwilson.deployment.jaxrs.Orders;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 * @author jbuhacoff
 */
public class Deploy implements Command {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Deploy.class);

    private Configuration options = null;
    
    @Override
    public void setOptions(Configuration options) {
        this.options = options;
    }
    
    @Override
    public void execute(String[] args) throws Exception {
        if( options == null ) { options = new PropertiesConfiguration(); }
        String jsonFilename = options.getString("json");
        if( jsonFilename == null ) {
            throw new IllegalArgumentException("Usage: deploy --json /path/to/deployment.json");
        }
        File jsonFile = new File(jsonFilename);
        if( !jsonFile.exists() ) {
            log.error("File not found: {}", jsonFile.getAbsolutePath());
            throw new FileNotFoundException("File not found");
        }
        try( InputStream in = new FileInputStream(jsonFile) ) {
            ObjectMapper mapper = new ObjectMapper();
            OrderDocument order = mapper.readValue(in, OrderDocument.class);
            
            // TODO:  handle the request
//            Orders orders = new Orders();
//            orders.createOne(locator,order,request,response);
            System.out.println(mapper.writeValueAsString(order));
            
        }
    }
    
}
