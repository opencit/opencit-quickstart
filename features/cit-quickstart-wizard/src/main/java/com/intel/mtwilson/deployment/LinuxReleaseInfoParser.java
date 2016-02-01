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
public class LinuxReleaseInfoParser {
    private static final Pattern distributorPattern = Pattern.compile("^Distributor ID:\\s+([\\w\\d ]+)$", Pattern.MULTILINE);
    private static final Pattern versionPattern = Pattern.compile("^Release:\\s+([\\w\\d \\.]+)$", Pattern.MULTILINE);

    /*
     * Given text output of the "lsb_release -a" command in Linux,
     * this method will return the distributor and release information.
     */
    public OperatingSystemInfo parse(String lsbReleaseText) {
        String distributor = null;
        String version = null;
        Matcher distributorMatcher = distributorPattern.matcher(lsbReleaseText);
        Matcher versionMatcher = versionPattern.matcher(lsbReleaseText);
        if (distributorMatcher.find()) {
            distributor = distributorMatcher.group(1);
        }
        if (versionMatcher.find()) {
            version = versionMatcher.group(1);
        }
        return new OperatingSystemInfo(distributor, version);
    }
    
}
