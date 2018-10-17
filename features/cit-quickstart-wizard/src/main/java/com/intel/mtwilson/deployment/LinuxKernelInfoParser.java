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
public class LinuxKernelInfoParser {
    private static final Pattern versionPattern = Pattern.compile("^([^-]+)-?(.*)$");
    /*
     * Given text output of the "lsb_release -a" command in Linux,
     * this method will return the distributor and release information.
     */
    public LinuxKernelInfo parse(String unameText) {
        String version = null;
        String build = null;
        Matcher versionMatcher = versionPattern.matcher(unameText);
        if (versionMatcher.find()) {
            version = versionMatcher.group(1);
            build = versionMatcher.group(2);
        }
        return new LinuxKernelInfo(version, build);
    }
    
}
