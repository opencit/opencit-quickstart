/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import java.util.List;

/**
 * Implemented by pre-configuration tasks that generate env files and need
 * to copy them to the target host. 
 * @author jbuhacoff
 */
public interface FileTransferManifestProvider {
    List<FileTransferDescriptor> getFileTransferManifest();
}
