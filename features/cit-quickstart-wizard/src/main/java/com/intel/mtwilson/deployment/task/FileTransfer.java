/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.dcsg.cpg.performance.Progress;
import com.intel.mtwilson.deployment.FileTransferDescriptor;
import com.intel.mtwilson.deployment.FileTransferManifestProvider;
import com.intel.mtwilson.deployment.SSHClientWrapper;
import com.intel.mtwilson.deployment.descriptor.SSH;
import com.intel.mtwilson.deployment.jaxrs.faults.Connection;
import com.intel.mtwilson.deployment.jaxrs.faults.FileNotFound;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.TransferListener;
import org.apache.commons.lang3.StringUtils;

/**
 * Copy an installer to a remote host
 *
 * @author jbuhacoff
 */
public class FileTransfer extends AbstractTaskWithId implements Progress {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileTransfer.class);
    private SSH remote;
    private List<FileTransferDescriptor> manifest;
    private FileTransferManifestProvider manifestProvider;
    private HashMap<FileTransferDescriptor, FileTransferProgressListener> listenerMap = new HashMap<>();

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

    public FileTransfer(SSH remote, FileTransferManifestProvider manifestProvider) {
        super();
        this.remote = remote;
        this.manifestProvider = manifestProvider;
    }

    @Override
    public void execute() {
        // precondition:  file transfer manifest available and all source files exist and readable
        if (manifest == null) {
            manifest = manifestProvider.getFileTransferManifest();
            for (FileTransferDescriptor entry : manifest) {
                if (!entry.getSource().exists() || !entry.getSource().canRead()) {
                    fault(new FileNotFound(entry.getSource().getName()));
                }
            }
        }

        // prepare all the file transfer listeners
        for (FileTransferDescriptor entry : manifest) {
            listenerMap.put(entry, new FileTransferProgressListener(entry.getSource()));
        }

        try (SSHClientWrapper client = new SSHClientWrapper(remote)) {
            client.connect();
            try (SFTPClient sftp = client.sftp()) {

                for (FileTransferDescriptor entry : manifest) {
                    FileSystemFile localfile = new FileSystemFile(entry.getSource());

                    SFTPFileTransfer transfer = sftp.getFileTransfer();
//                    ProgressTransferListener listener = new ProgressTransferListener(this);
                    transfer.setTransferListener(listenerMap.get(entry));
                    log.debug("Uploading file: {} to target host: {} path: {}", entry.getSource().getAbsolutePath(), remote.getHost(), entry.getTargetPath());
                    transfer.upload(localfile, entry.getTargetPath());

                    // TODO:  set permissions on remote file if entry.getPermissions() != null 
                }

            }
            client.disconnect();
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
    public static class FileTransferProgressListener implements TransferListener, StreamCopier.Listener {

        private File file;
        private long progress, progressMax;

        public FileTransferProgressListener(File file) {
            log.debug("File transfer progress listener initialized with file: {} size: {}", file.getAbsolutePath(), file.length());
            this.file = file;
            this.progress = 0;
            this.progressMax = file.length();
        }

        /**
         * Listens for progress updates from SFTPClient and updates our file
         * transfer status by setting current (completed) to number of bytes
         * transferred.
         *
         * @param transferred
         * @throws IOException
         */
        @Override
        public void reportProgress(long transferred) throws IOException {
            progress = transferred;
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
        public TransferListener directory(String path) {
            log.debug("File transfer progress listener directory: {}", path);
            return this;
        }

        @Override
        public StreamCopier.Listener file(String filename, long size) {
            log.debug("File transfer progress listener file: {} size; {}", filename, size);
            this.progress = 0;
            this.progressMax = size;
            return this;
        }
    }
}
