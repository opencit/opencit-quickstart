/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jbuhacoff
 */
public class DependenciesTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DependenciesTest.class);

    @Test
    public void testSortDependencies() throws JsonProcessingException {
        SoftwarePackageExample a = new SoftwarePackageExample("a");
        SoftwarePackageExample b = new SoftwarePackageExample("b");
        SoftwarePackageExample c = new SoftwarePackageExample("c");
        SoftwarePackageExample d = new SoftwarePackageExample("d");
        SoftwarePackageExample e = new SoftwarePackageExample("e");
        a.getDependencies().add(b);
        b.getDependencies().add(c);
        c.getDependencies().add(e);
        d.getDependencies().add(b);
        ArrayList<SoftwarePackageExample> list = new ArrayList<>();
        list.addAll(Arrays.asList(a, b, c, d, e));

        ObjectMapper mapper = new ObjectMapper();

        for (int i = 0; i < 10; i++) {
            Collections.shuffle(list);
            log.debug("shuffled: {}", mapper.writeValueAsString(list));
            Collections.sort(list, new DependencyComparator<SoftwarePackageExample>());
            log.debug("sorted: {}", mapper.writeValueAsString(list));
            postcondition(list);
        }
    }

    /**
     * Checks that each element in list appears AFTER all its dependencies.
     *
     * @param list
     */
    private void postcondition(List<SoftwarePackageExample> list) {
        log.debug("Checking postcondition...");
        HashSet<SoftwarePackageExample> seen = new HashSet<>();
        for (SoftwarePackageExample item : list) {
            log.debug("Checking item: {}", item.getName());
            Collection<SoftwarePackageExample> dependencies = item.getDependencies();
            for (SoftwarePackageExample dependency : dependencies) {
                assertTrue(seen.contains(dependency));
            }
            seen.add(item);
        }
    }

    public static class SoftwarePackageExample implements Dependencies<SoftwarePackageExample> {

        private ArrayList<SoftwarePackageExample> dependencies = new ArrayList<>();
        private String name;

        public SoftwarePackageExample(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @JsonIgnore
        @Override
        public Collection<SoftwarePackageExample> getDependencies() {
            return dependencies;
        }
    }
}
