/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.mtwilson.util.task.Task;

/**
 *
 * @author jbuhacoff
 */
public class TaskUtil {
    public static <T> T unwrap(Class<T> clazz, Task task) {
        Task target = task;
        while(target != null) {
            if( clazz.isInstance(target) ) {
                return (T)target;
            }
            if( target instanceof TaskDecorator ) {
                target = ((TaskDecorator)target).getDelegate();
            }
            else {
                target = null;
            }
        }
        return null;
    }
}
