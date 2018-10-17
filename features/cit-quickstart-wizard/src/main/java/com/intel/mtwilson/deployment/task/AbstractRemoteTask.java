/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.ssh.Exit;
import com.intel.mtwilson.deployment.ssh.FileOutput;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author jbuhacoff
 */
public abstract class AbstractRemoteTask extends AbstractTaskWithId {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractRemoteTask.class);
    private int sequence = 0;

    /*
     protected Result sshexec(SSHClientWrapper clientWrapper, String command, int timeout, TimeUnit timeoutUnits) throws ConnectionException, TransportException, IOException {
     return sshexec(clientWrapper.client(), command, timeout, timeoutUnits);
     }
     * */
    protected Result sshexec(SSHClientWrapper clientWrapper, String command) throws Exception {
//        return sshexec(clientWrapper.client(), command);
        int commandId = sequence();
        File taskDirectory = getTaskDirectory();
        if (!taskDirectory.exists()) {
            taskDirectory.mkdirs();
        }
        String taskDirectoryPath = taskDirectory.getAbsolutePath();
        log.debug("sshexec starting session {} for command: {}", commandId, command);
        File stdoutLogFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stdout.log");
        File stderrLogFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stderr.log");
        FileOutput output = new FileOutput(stdoutLogFile, stderrLogFile);
        // execute the command, with factory-configured retry attempts and intervals 
        log.debug("sshexec executing command #{}: {}", commandId, command);
        Exit status = clientWrapper.execute(command, output); // throws Exception
        // read the output files 
        String stdoutText = "", stderrText = "";
        if (stdoutLogFile.exists()) {
            stdoutText = FileUtils.readFileToString(stdoutLogFile, "UTF-8"); // throws IOException
        }
        if (stderrLogFile.exists()) {
            stderrText = FileUtils.readFileToString(stdoutLogFile, "UTF-8"); // throws IOException

        }
        Result result = new Result(status.getCode(), stdoutText, stderrText);
        return result;
    }
/*
    private Result sshexec(SSHClient client, String command) throws ConnectionException, TransportException, IOException {
        log.debug("sshexec to host {}", client.getRemoteHostname());
        int tries = 1, maxTries = 10;
        while (tries <= maxTries) {
            try {
                Result result = sshexec(client, command, 2, TimeUnit.MINUTES); // TODO: make default timeout configurable
                return result;
            } catch (UserAuthException e) {
                log.error("Authentication failed when connecting to {}", client.getRemoteHostname());
                throw e;
            } catch (IOException e) {
                log.error("Failed to connect to {}: {} [attempt {} of {}]", client.getRemoteHostname(), e.getMessage(), tries, maxTries);
                if (tries == maxTries) {
                    throw e;
                }
            }
            tries++;
        }
        log.error("Failed to connect to {} to execute comamnd: {}", client.getRemoteHostname(), command);
        throw new IllegalStateException("sshexec max retries exceeded");
    }
*/
    /*
    private Result sshexec(SSHClient client, String command, int timeout, TimeUnit timeoutUnits) throws ConnectionException, TransportException, IOException {
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
            try (FileOutputStream stdoutLog = new FileOutputStream(stdoutLogFile); FileOutputStream stderrLog = new FileOutputStream(stderrLogFile)) {
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
            }
            log.debug("waiting with timeout for command to finish");
            sshresult.join(timeout, timeoutUnits);

//             String stdoutText = IOUtils.toString(stdout, "UTF-8"); // throws IOException
//             String stderrText = IOUtils.toString(stderr, "UTF-8"); // throws IOException
//             log.debug("result: {}", stdoutText);

             // log the output
             // store the stdout into a file
//             File stdoutFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stdout.log");
//             FileUtils.writeStringToFile(stdoutFile, stdoutText, Charset.forName("UTF-8"));

             // store the stderr into a file
//             File stderrFile = new File(taskDirectoryPath + File.separator + String.valueOf(commandId) + ".stderr.log");
//             FileUtils.writeStringToFile(stderrFile, stderrText, Charset.forName("UTF-8"));

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
    */

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
