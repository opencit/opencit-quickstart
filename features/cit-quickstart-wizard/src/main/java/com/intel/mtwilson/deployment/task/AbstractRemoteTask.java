/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author jbuhacoff
 */
public abstract class AbstractRemoteTask extends AbstractTaskWithId {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractRemoteTask.class);
    private int sequence = 0;

    protected Result sshexec(Session session, String command) throws ConnectionException, TransportException, IOException {
        int commandId = sequence();

        Session.Command sshresult = session.exec(command); // throws ConnectionException, TransportException
        InputStream stdout = sshresult.getInputStream();
        InputStream stderr = sshresult.getErrorStream();
        String stdoutText = IOUtils.toString(stdout, "UTF-8"); // throws IOException
        String stderrText = IOUtils.toString(stderr, "UTF-8"); // throws IOException
        log.debug("result: {}", stdoutText);

        // log the output
        // store the stdout into a file
        File stdoutFile = new File(Folders.repository("tasks") + File.separator + getId() + File.separator + String.valueOf(commandId) + ".stdout.log");
        FileUtils.writeStringToFile(stdoutFile, stdoutText, Charset.forName("UTF-8"));

        // store the stderr into a file
        File stderrFile = new File(Folders.repository("tasks") + File.separator + getId() + File.separator + String.valueOf(commandId) + ".stderr.log");
        FileUtils.writeStringToFile(stderrFile, stderrText, Charset.forName("UTF-8"));


        Result result = new Result(sshresult.getExitStatus(), stdoutText, stderrText);
        return result;

    }

    /**
     * A task may perform multiple internal steps that may have associated
     * temporary data to be stored in the task's temporary directory.
     *
     * Call the sequence() method to increment the internal step number and
     * return the new number. Use the returned sequence number as the prefix for
     * temporary files stored in the task output directory to avoid collisions
     * with other steps and make it easier for someone to inspect the temporary
     * data.
     *
     * @return
     */
    protected int sequence() {
        return ++sequence;
    }
}
