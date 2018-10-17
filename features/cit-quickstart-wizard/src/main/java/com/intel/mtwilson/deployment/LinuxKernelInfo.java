/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jbuhacoff
 */
public class LinuxKernelInfo {
    private static final Pattern versionDetailPattern = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    private String version;
    private String build;
    private String versionMajor = null;
    private String versionMinor = null;
    private String versionPatch = null;

    public LinuxKernelInfo(String version, String build) {
        this.version = version;
        this.build = build;
    }

    public String getVersion() {
        return version;
    }

    public String getBuild() {
        return build;
    }

    private void parseVersion() {
        if (version == null) {
            return;
        }
        Matcher versionDetailMatcher = versionDetailPattern.matcher(version);
        if (versionDetailMatcher.find()) {
            versionMajor = versionDetailMatcher.group(1);
            versionMinor = versionDetailMatcher.group(2);
            versionPatch = versionDetailMatcher.group(3);
        }
    }

    public String getVersionMajor() {
        if (versionMajor == null) {
            parseVersion();
        }
        return versionMajor;
    }

    public String getVersionMinor() {
        if (versionMinor == null) {
            parseVersion();
        }
        return versionMinor;
    }

    public String getVersionPatch() {
        if (versionPatch == null) {
            parseVersion();
        }
        return versionPatch;
    }
    
}
