/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.ssh;

import net.schmizz.sshj.connection.channel.direct.Signal;

/**
 *
 * @author jbuhacoff
 */
public class Exit {
    private Integer code;
    private String message;
    private Signal signal;

    public Exit(Integer code, String message, Signal signal) {
        this.code = code;
        this.message = message;
        this.signal = signal;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Signal getSignal() {
        return signal;
    }
    
    
}
