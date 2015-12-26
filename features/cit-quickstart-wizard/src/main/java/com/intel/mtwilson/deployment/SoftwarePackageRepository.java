/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public interface SoftwarePackageRepository {
    SoftwarePackage searchByNameEquals(String name);
    List<SoftwarePackage> listAll();
}
