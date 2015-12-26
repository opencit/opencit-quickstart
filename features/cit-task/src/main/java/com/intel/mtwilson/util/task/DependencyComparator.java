/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import java.util.Collection;
import java.util.Comparator;

/**
 * Orders two tasks according to dependency: using this comparator, tasks
 * are ordered from first to last with dependencies first and dependent tasks
 * last. This can be used to sort a list of tasks into execution order.
 * This comparator assumes that tasks do NOT have circular dependencies.
 * @author jbuhacoff
 */
public class DependencyComparator<T extends Dependencies> implements Comparator<Dependencies<T>> {

    /**
     * 
     * @param task1
     * @param task2
     * @return -1 if task1 should run before task2, 0 if they can run in any order, 1 if task1 should run after task2
     */
    @Override
    public int compare(Dependencies<T> task1, Dependencies<T> task2) {
        if( task1 == null || task2 == null ) { throw new NullPointerException(); }
        if( isDependent(task1,task2) ) { return 1; }
        if( isDependent(task2,task1) ) { return -1; }
        return 0;
    }
    
    /**
     * 
     * @param subject must not be null
     * @param other can be null;   isDependent(x,null) is false for any x
     * @return true if subject is dependent on other, or in other words, if other appears in the dependency graph of subject
     */
    private boolean isDependent(Dependencies<T> subject, Dependencies<T> other) {
        if( subject == null ) { throw new NullPointerException(); }
        if( other == null ) { return false; }
        Collection<T> dependencies = subject.getDependencies();
        if( dependencies == null ) { return false; }
        for(T t : dependencies) {
            if( t == null ) { continue; }
            if( t.equals(other) ) {
                // subject directly dependent on other
                return true;
            }
            if( isDependent(t, other) ) {
                // subject indirectly dependent on other, through t
                return true;
            }
        }
        // subject is not directly or transitively dependent on other task
        return false;
    }
}
