/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class DependenciesUtil {
        /**
     * Invariant: after each iteration "i+1" of the outer loop, the number of items
     * in the list that have dependencies AFTER them in the list is smaller 
     * than it was in iteration "i" (the previous iteration) or the loop counter
     * i is incremented.
     * @param list 
     */
    public static <T extends Dependencies<T>> void sort(List<T> list) {
        int i = 0, length = list.size(), highestDependencyIndex, currentDependencyIndex;
        T highestDependency;
        while (i < length) {
            Collection<T> dependencies = list.get(i).getDependencies();
            // if this item has dependencies, identify which one has the highest index in the list
            if (dependencies != null) {
                highestDependency = null;
                highestDependencyIndex = 0;
                for (T dependency : dependencies) {
                    currentDependencyIndex = list.indexOf(dependency);
                    if (currentDependencyIndex > highestDependencyIndex) {
                        highestDependencyIndex = currentDependencyIndex;
                        highestDependency = dependency;
                    }
                }
                // if the dependency with the highest index has a higher index than this item, then insert it right before this item and process it
                if( highestDependencyIndex > i ) {
                    list.remove(highestDependencyIndex);
                    list.add(i, highestDependency);
                    continue;
                }
            }
            // otherwise... either no dependencies or all of them are before this item in the list, go on to next item
            i++;
        }
    }
    
        /**
     * 
     * @param subject must not be null
     * @param other can be null;   isDependent(x,null) is false for any x
     * @return true if subject is dependent on other, or in other words, if other appears in the dependency graph of subject
     */
    public static <T extends Dependencies<T>> boolean isDependent(Dependencies<T> subject, Dependencies<T> other) {
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
