/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.deployment.task.PostconfigureOpenstack.OpenstackRole;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class ParseOpenstackRoleTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ParseOpenstackRoleTest.class);

    /**
     * Parse output of this command:
     * <pre>
     * openstack role list --project projectName --user username -f json
     * </pre>
     * @throws IOException 
     */
    @Test
    public void testParseTwoRoles() throws IOException {
        String json = "[{\"ID\": \"9fe2ff9ee4384b1894a90878d3e92bab\", \"Name\": \"_member_\", \"Project\": \"cit\", \"User\": \"cit-admin\"}, {\"ID\": \"d5b0ec4ce8d54e0fa668f07f69d5812d\", \"Name\": \"admin\", \"Project\": \"cit\", \"User\": \"cit-admin\"}]";
        ObjectMapper mapper = new ObjectMapper();
        OpenstackRole[] roles = mapper.readValue(json, OpenstackRole[].class);
        for(OpenstackRole role : roles) {
            log.debug("Project {} role {} user {}", role.project, role.name, role.user);
        }
    }
}
