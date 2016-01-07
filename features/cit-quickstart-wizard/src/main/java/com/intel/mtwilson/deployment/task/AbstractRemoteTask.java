/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.performance.AlarmClock;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Signal;
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

    protected Result sshexec(SSHClientWrapper clientWrapper, String command, int timeout, TimeUnit timeoutUnits) throws ConnectionException, TransportException, IOException {
        return sshexec(clientWrapper.client(), command, timeout, timeoutUnits);
    }

    protected Result sshexec(SSHClientWrapper clientWrapper, String command) throws ConnectionException, TransportException, IOException {
        return sshexec(clientWrapper.client(), command);
    }

    protected Result sshexec(SSHClient client, String command) throws ConnectionException, TransportException, IOException {
        return sshexec(client, command, 2, TimeUnit.MINUTES); // default 2 minutes timeout; TODO: make it configurable
    }

    protected Result sshexec(SSHClient client, String command, int timeout, TimeUnit timeoutUnits) throws ConnectionException, TransportException, IOException {
        int commandId = sequence();
        File taskDirectory = getTaskDirectory();
        if (!taskDirectory.exists()) {
            taskDirectory.mkdirs();
        }
        String taskDirectoryPath = taskDirectory.getAbsolutePath();
        log.debug("sshexec starting session for command: {}", command);
        try (Session session = client.startSession()) {
            Session.Command sshresult = session.exec(command); // throws ConnectionException, TransportException
            InputStream stdout = sshresult.getInputStream();
            InputStream stderr = sshresult.getErrorStream();
            int stdoutAvailable = stdout.available();
            int stderrAvailable = stderr.available();
            int stdoutReceived = 0, stderrReceived = 0;
            File stdoutLogFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stdout.log");
            File stderrLogFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stderr.log");
            FileOutputStream stdoutLog = new FileOutputStream(stdoutLogFile);
            FileOutputStream stderrLog = new FileOutputStream(stderrLogFile);
            AlarmClock delay = new AlarmClock(200, TimeUnit.MILLISECONDS);
            //while ( stdoutAvailable > 0 || stdoutReceived > -1 || stderrAvailable > 0 || stderrReceived > -1) {
            while (stdoutReceived > -1 || stderrReceived > -1) {
                log.debug("stdout avail: {} recvd: {}   stderr avail: {} recvd: {}", stdoutAvailable, stdoutReceived, stderrAvailable, stderrReceived);
                stdoutAvailable = stdout.available();
                stderrAvailable = stderr.available();
//                if (stdoutAvailable > 0) {
                log.debug("reading stdout: {}", stdoutAvailable);
                byte[] stdoutBuffer = new byte[stdoutAvailable];
                stdoutReceived = stdout.read(stdoutBuffer, 0, stdoutAvailable);
                if (stdoutReceived > -1) {
                    log.debug("writing stdout log: {}", stdoutReceived);
                    stdoutLog.write(stdoutBuffer, 0, stdoutReceived);
                }
//                }
//                if (stderrAvailable > 0) {
                log.debug("reading stderr: {}", stderrAvailable);
                byte[] stderrBuffer = new byte[stderrAvailable];
                stderrReceived = stderr.read(stderrBuffer, 0, stderrAvailable);
                if (stderrReceived > -1) {
                    log.debug("writing stderr log: {}", stderrReceived);
                    stderrLog.write(stderrBuffer, 0, stderrReceived);
                }
//                }
                delay.sleep();
            }
            stdoutLog.close();
            stderrLog.close();
            log.debug("waiting with timeout for command to finish");
            sshresult.join(timeout, timeoutUnits);

            /*
             String stdoutText = IOUtils.toString(stdout, "UTF-8"); // throws IOException
             String stderrText = IOUtils.toString(stderr, "UTF-8"); // throws IOException
             log.debug("result: {}", stdoutText);

             // log the output
             // store the stdout into a file
             File stdoutFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stdout.log");
             FileUtils.writeStringToFile(stdoutFile, stdoutText, Charset.forName("UTF-8"));

             // store the stderr into a file
             File stderrFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stderr.log");
             FileUtils.writeStringToFile(stderrFile, stderrText, Charset.forName("UTF-8"));
             */
            Integer exitStatus = sshresult.getExitStatus();
            String errorMessage = sshresult.getExitErrorMessage();
            if (errorMessage != null) {
                log.debug("Command error message: {}", errorMessage);
                File errorMessageFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".message.log");
                FileUtils.writeStringToFile(errorMessageFile, errorMessage, Charset.forName("UTF-8"));
                if (exitStatus == null) {
                    exitStatus = 1;
                }
            }
            log.debug("Command core dumped? {}", sshresult.getExitWasCoreDumped());
            Signal exitSignal = sshresult.getExitSignal();
            if (exitSignal != null) {
                log.debug("Command exit signal: {}", exitSignal.name());
                File signalFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".signal.log");
                FileUtils.writeStringToFile(signalFile, exitSignal.toString(), Charset.forName("UTF-8"));
                if (exitStatus == null) {
                    exitStatus = 1;
                }

            }
            if (exitStatus == null) {
                log.debug("Command exit status is null, assuming 0");
                exitStatus = 0;
            }

            String stdoutText = "", stderrText = "";
            if (stdoutLogFile.exists()) {
                stdoutText = FileUtils.readFileToString(stdoutLogFile, "UTF-8");
            }
            if (stderrLogFile.exists()) {
                stderrText = FileUtils.readFileToString(stdoutLogFile, "UTF-8");

            }
            return new Result(exitStatus, stdoutText, stderrText);
        }
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
