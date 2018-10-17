/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class DependentTaskSortTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DependentTaskSortTest.class);

    private List<Task> createTasks() {
        Hello a = new Hello("a");
        Hello b = new Hello("b");
        Hello c = new Hello("c");
        Hello d = new Hello("d");
        Hello e = new Hello("e");
        Hello f = new Hello("f");
        Hello g = new Hello("g");
        a.getDependencies().add(b);
        a.getDependencies().add(c);
        b.getDependencies().add(c);
        b.getDependencies().add(e);
        c.getDependencies().add(d);
        c.getDependencies().add(e);
        e.getDependencies().add(f);
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(a);
        tasks.add(b);
        tasks.add(c);
        tasks.add(d);
        tasks.add(e);
        tasks.add(f);
        tasks.add(g);
        return tasks;
    }

    private void printTasks(List<Task> tasks) {
        for (Task t : tasks) {
            String name = t.toString();
            String dependencies = toString(t.getDependencies());
            log.debug("task: {}  depends on: {}", name, dependencies);
        }
    }

    private List<String> getNames(Collection<Task> tasks) {
        ArrayList<String> names = new ArrayList<>();
        for (Task t : tasks) {
            names.add(t.toString());
        }
        return names;
    }

    private String toString(Collection<Task> tasks) {
        if (tasks == null) {
            return null;
        }
        return StringUtils.join(getNames(tasks), ',');
    }

    @Test
    public void testSortTasksByDependencies() {
        List<Task> tasks = createTasks();
        Collections.shuffle(tasks);
        printTasks(tasks);
        DependenciesUtil.sort(tasks);
        log.debug("sorted tasks");
        printTasks(tasks);
    }

    public static class Hello extends AbstractTask {

        private String name;

        public Hello(String name) {
            this.name = name;
        }

        @Override
        public void execute() {
            log.debug("hello: {}", toString());
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
