/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import java.util.Collection;

/**
 *
 * @author jbuhacoff
 */
public interface Preconditions {
    Collection<Condition> getPreconditions();
}
