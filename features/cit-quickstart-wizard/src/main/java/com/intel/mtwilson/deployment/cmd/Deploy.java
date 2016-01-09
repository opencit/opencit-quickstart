/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.console.Command;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import com.intel.mtwilson.jaxrs2.client.JaxrsClient;
import com.intel.mtwilson.jaxrs2.client.JaxrsClientBuilder;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Note: this command requires the server to be running; it uses the /orders API
 * to submit an order from the command line
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
        if (options == null) {
            options = new PropertiesConfiguration();
        }
        String jsonFilename = options.getString("json");
        if (jsonFilename == null) {
            throw new IllegalArgumentException("Usage: deploy --json /path/to/order.json");
        }
        File jsonFile = new File(jsonFilename);
        if (!jsonFile.exists()) {
            log.error("File not found: {}", jsonFile.getAbsolutePath());
            throw new FileNotFoundException("json");
        }

        File httpsPropertiesFile = new File(Folders.configuration() + File.separator + "https.properties");
        if (!httpsPropertiesFile.exists()) {
            log.error("File not found: {}", httpsPropertiesFile.getAbsolutePath());
            throw new FileNotFoundException("https.properties");
        }
        // read in our own tls cert sha1 hash from https.properties
        Properties httpsProperties = new Properties();
        try (FileInputStream httpsPropertiesIn = new FileInputStream(httpsPropertiesFile)) {
            httpsProperties.load(httpsPropertiesIn);
        }

        com.intel.dcsg.cpg.configuration.Configuration configuration = ConfigurationFactory.getConfiguration();
        String port = configuration.get("jetty.secure.port", "443");
        String endpointUrl = "https://127.0.0.1:" + port; // TODO: read from configuration after it's fixed,  right now it's http://ip6-localhost which isn't useful

        try (InputStream in = new FileInputStream(jsonFile)) {
            ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
            OrderDocument order = mapper.readValue(in, OrderDocument.class);


            Properties quickstartProperties = new Properties();
            quickstartProperties.setProperty("endpoint.url", endpointUrl);
            quickstartProperties.setProperty("tls.policy.certificate.sha1", httpsProperties.getProperty("tls.cert.sha1"));
            JaxrsClient keybrokerClient = JaxrsClientBuilder.factory().configuration(quickstartProperties).build();
            Response response = keybrokerClient.getTargetPath("/v1/quickstart/orders").request().post(Entity.json(order));
            log.debug("Response status: {}", response.getStatus());
            log.debug("Response: {}", mapper.writeValueAsString(order));
            if (response.getStatus() == 200) {
                OrderDocument responseOrderDocument = response.readEntity(OrderDocument.class);
                // assume status will be available at this URL for now. TODO: get URL from response
                System.out.println(endpointUrl + "/v1/quickstart/orders/" + responseOrderDocument.getId().toString());
            }
        }
    }
}
