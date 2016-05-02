/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.crypto.digest.Digest;
import com.intel.dcsg.cpg.performance.Observer;
import com.intel.dcsg.cpg.performance.Progress;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.deployment.jaxrs.faults.FileNotFound;
import com.intel.mtwilson.util.exec.Result;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Copy an installer to a remote host
 *
 * @author jbuhacoff
 */
public class FileTransfer extends AbstractRemoteTask implements Progress {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileTransfer.class);
    private SSH remote;
    protected List<FileTransferDescriptor> manifest;
    private HashMap<FileTransferDescriptor, FileTransferProgressListener> listenerMap = new HashMap<>();

    public FileTransfer(SSH remote) {
        super();
        this.remote = remote;
    }
    
    public FileTransfer(SSH remote, FileTransferDescriptor singleFileTransfer) {
        super();
        this.remote = remote;
        this.manifest = new ArrayList<>();
        this.manifest.add(singleFileTransfer);
    }

    public FileTransfer(SSH remote, List<FileTransferDescriptor> manifest) {
        super();
        this.remote = remote;
        this.manifest = manifest;
    }

    @Override
    public void execute() {
        Configuration configuration;
        try {
            configuration = ConfigurationFactory.getConfiguration();
        } catch (IOException e) {
            log.error("Cannot load configuration", e);
            configuration = new PropertiesConfiguration();
        }

        // precondition:  file transfer manifest available and all source files exist and readable
        for (FileTransferDescriptor entry : manifest) {
            if (!entry.getSource().exists() || !entry.getSource().canRead()) {
                fault(new FileNotFound(entry.getSource().getName()));
            }
        }

        // prepare all the file transfer listeners
        for (FileTransferDescriptor entry : manifest) {
            listenerMap.put(entry, new FileTransferProgressListener(entry.getSource()));
        }

        // a map to track which files should be skipped, if etag is enabled        
        HashSet<FileTransferDescriptor> etagMatches = null;
        boolean etagEnabled = Boolean.valueOf(configuration.get("mtwilson.quickstart.filetransfer.etag", "true")).booleanValue();

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {

            if (etagEnabled) {
                etagMatches = new HashSet<>();
                log.debug("FileTransfer etag enabled");
                log.debug("FileTransfer opened session");
                for (FileTransferDescriptor entry : manifest) {
                    log.debug("FileTransfer processing entry for etag: {}", entry.getTargetPath());

                    // check the etag of the local file and the remote file... if same then we skip the file transfer
                    // get sha256 sum of local file.  if the cached etag is not newer than the file, then we
                    // calculat it again.
                    String etag;
                    final File source = entry.getSource();
                    log.debug("Source file: {}", source.getAbsolutePath());
                    File sourceEtag = new File(source.getAbsolutePath() + ".sha256");
                    if (sourceEtag.exists() && sourceEtag.lastModified() > source.lastModified()) {
                        log.debug("Reading etag file");
                        etag = FileUtils.readFileToString(sourceEtag, "UTF-8").replaceAll("\\s", "");
                        log.debug("Read etag from file: {}", sourceEtag.getAbsolutePath());
                    } else {
                        log.debug("Generating etag for file: {}", source.getAbsolutePath());
                        etag = Digest.sha256().digest(new FileInputStream(source)).toHex();
                        FileUtils.writeStringToFile(sourceEtag, etag, "UTF-8");
                        log.debug("Stored etag in file: {}", sourceEtag.getAbsolutePath());
                    }

                    // get sha256 sum of remote file
                    Result result = sshexec(client, "/usr/bin/sha256sum " + entry.getTargetPath() + " | /usr/bin/awk '{print $1}'");
                    if (result.getExitCode() == 0) {
                        String remoteEtag = result.getStdout().replaceAll("\\s", "");
                        log.debug("File: {} Local etag: {} Remote etag: '{}'", entry.getTargetPath(), etag, remoteEtag);
                        if (etag.equalsIgnoreCase(remoteEtag)) {
                            log.debug("Remote etag matches file: {} with etag: {}", source.getName(), etag);
                            listenerMap.get(entry).observe(new Progress() {
                                @Override
                                public long getCurrent() {
                                    return source.length();
                                }

                                @Override
                                public long getMax() {
                                    return source.length();
                                }
                            });
                            etagMatches.add(entry); // will cause it to be skipped later in upload section
                        } else {
                            log.debug("Remote etag does NOT match file: {} with etag: {}", source.getName(), etag);
                        }
                    } else {
                        log.error("Cannot get SHA-256 of remote file, stdout: {}  stderr: {}", result.getStdout(), result.getStderr());
                    }
                }

            }

            for (FileTransferDescriptor entry : manifest) {
                log.debug("FileTransfer processing entry for upload: {}", entry.getTargetPath());

                if (etagEnabled && etagMatches != null && etagMatches.contains(entry)) {
                    log.debug("FileTransfer skipping entry because etag matches; {}", entry.getTargetPath());
                    continue;
                }

                log.debug("Uploading file: {} to target host: {} path: {}", entry.getSource().getAbsolutePath(), remote.getHost(), entry.getTargetPath());
                client.upload(entry.getSource(), entry.getTargetPath(), listenerMap.get(entry));

                // TODO:  set permissions on remote file if entry.getPermissions() != null 
            }

        } catch (Exception e) {
            log.error("Connection failed", e);
            fault(new Connection(remote.getHost()));
        }
    }

    public String getFilenameCsv() {
        ArrayList<String> names = new ArrayList<>();
        if (manifest != null) {
            for (FileTransferDescriptor fileTransfer : manifest) {
                names.add(fileTransfer.getSource().getName());
            }
        }
        return StringUtils.join(names, ", ");
    }

    public String getHost() {
        return remote.getHost();
    }

    /**
     * If using a manifest provider, this method will only return a non-null
     * value AFTER execute()
     *
     * @return
     */
    public List<FileTransferDescriptor> getFileTransferManifest() {
        return manifest;
    }

    @Override
    public long getCurrent() {
        long current = 0;
        for (FileTransferProgressListener listener : listenerMap.values()) {
            current += listener.getProgress();
        }
        return current;
    }

    @Override
    public long getMax() {
        long max = 0;
        for (FileTransferProgressListener listener : listenerMap.values()) {
            max += listener.getProgressMax();
        }
        return max;
    }

    /**
     * A bridge between our Task which implements Progress as well as the
     * StreamCopier.Listener interface, and the TransferListener interface that
     * SFTPClient expects.
     */
    public static class FileTransferProgressListener implements Observer<Progress> {

        private File file;
        private long progress, progressMax;

        public FileTransferProgressListener(File file) {
            log.debug("File transfer progress listener initialized with file: {} size: {}", file.getAbsolutePath(), file.length());
            this.file = file;
            this.progress = 0;
            this.progressMax = file.length();
        }

        public FileTransferProgressListener(LocalSourceFile file) {
            this.progress = 0;
            this.progressMax = file.getLength();
        }

        public File getFile() {
            return file;
        }

        public long getProgress() {
            return progress;
        }

        public long getProgressMax() {
            return progressMax;
        }

        @Override
        public void observe(Progress status) {
            progress = status.getCurrent();
            progressMax = status.getMax();
        }
    }
}
