/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task;

import com.intel.dcsg.cpg.io.file.FileOnlyFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.jaxrs2.Document;
import com.intel.mtwilson.repository.RepositoryCreateConflictException;
import com.intel.mtwilson.repository.RepositoryRetrieveException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * The JsonFileRepository stores files under a specified directory.
 * @author jbuhacoff
 */
public class JsonFileRepository {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonFileRepository.class);
    private final ObjectMapper mapper;
    private final File directory;

    /**
     * Create an instance with the specified directory for storing files.
     * 
     * @param path to directory in which to store JSON files, for example /opt/cit/repository/tasks
     * @throws FileNotFoundException if the directory does not exist and cannot be created
     */
    public JsonFileRepository(File directory) throws FileNotFoundException {
        this.directory = directory;
        if( !directory.exists() && !directory.mkdirs() ) {
            throw new FileNotFoundException(directory.getAbsolutePath());
        }
        mapper = new ObjectMapper();
    }

    /**
     * The identifiers returned in the list can be used as input to the other
     * methods in this class that accept a file identifier as a parameter.
     * 
     * @return list of the files in the directory;  directories are excluded
     */
    public List<String> list() {
        File[] files = directory.listFiles(new FileOnlyFilter()); // instead of DirectoryFilter 
        ArrayList<String> list = new ArrayList<>();
        for(File file : files) {
            list.add(file.getName());
        }
        return list;
    }
    
    /**
     * 
     * @param id must not be null
     * @return true if the directory contains a file named id
     */
    public boolean contains(String id) {
        File target = directory.toPath().resolve(id).toFile();
        return target.exists();
    }
    
    /**
     * Removes the file named id, if present. It is NOT an error if the
     * file does not already exist.
     * 
     * @param id must not be null
     */
    public void remove(String id) {
        File target = directory.toPath().resolve(id).toFile();
        if( target.exists() ) {
            delete(target);
        }
    }
    
    /**
     * Creates a new file with the given identifier and content. 
     * 
     * @param id to identify the file
     * @param content to store in the file
     * @throws IOException 
     * @throws RepositoryCreateConflictException if the file already exists
     */
    public void create(String id, String content) throws IOException {
        File target = directory.toPath().resolve(id).toFile(); 
        if( target.exists() ) { throw new RepositoryCreateConflictException(id); }
        try(OutputStream out = new FileOutputStream(target)) {
            IOUtils.write(content, out, "UTF-8");
        }
    }
    
    /**
     * Creates a new file with the value of {@code getId()} as the identifier 
     * and the serialized document instance as the content. 
     * 
     * @param instance
     * @throws IOException 
     * @throws RepositoryCreateConflictException if the file already exists
     */
    public void create(Document instance) throws IOException {
        create(instance.getId().toString(), mapper.writeValueAsString(instance));
    }
    
    /**
     * Store content in an existing file.
     * 
     * @param id to identify the file
     * @param content to store in the file
     * @throws IOException 
     */
    public void store(String id, String content) throws IOException {
        remove(id);
        create(id, content);
    }
    
    /**
     * Store the serialized instance in the file identified by {@code getId()}.
     * 
     * @param instance
     * @throws IOException 
     */
    public void store(Document instance) throws IOException {
        remove(instance.getId().toString());
        create(instance);
    }
    
    /**
     * Reads the content of the specified file and returns it as a String.
     * @param id
     * @return
     * @throws IOException 
     */
    public String retrieve(String id) throws IOException {
        File target = directory.toPath().resolve(id).toFile(); 
        if( !target.exists() ) { throw new RepositoryRetrieveException(id); }
        try(InputStream in = new FileInputStream(target)) {
            String content = IOUtils.toString(in, "UTF-8");
            return content;
        }
    }
    
    /**
     * Reads the content of the specified file, deserializes it, and returns
     * it as an instance of the specified class. 
     * @param <T>
     * @param id
     * @param clazz
     * @return
     * @throws IOException if file cannot be read or cannot be deserialized
     * into an instance of the specified class.
     */
    public <T> T retrieve(String id, Class<T> clazz) throws IOException {
        String content = retrieve(id);
        return mapper.readValue(content, clazz);
    }

    /**
     * Deletes files and directories recursively. 
     * 
     * @param file or directory must not be null and must exist.
     * @return true if the file or directory was successfully deleted
     */
    private boolean delete(File file) {
        assert file != null && file.exists();
        if( file.isFile() ) {
            return file.delete();
        }
        else if( file.isDirectory() ) {
            String name = file.getName();
            assert name != null;
            if( name.equals(".") || name.equals("..") ) {
                log.debug("Skipping special directory: {}", name);
                return true;
            }
            log.debug("Deleting contents of directory: {}", file.getAbsolutePath());
            boolean ok = true;
            File[] items = file.listFiles();
            for(File item : items) {
                boolean deleted = delete(item);
                if( !deleted ) {
                    log.error("Failed to delete file or directory: {}", item.getAbsolutePath());
                }
                ok = ok && deleted;
            }
            return ok;
        }
        else {
            log.error("Failed to delete non-file non-directory: {}", file.getAbsolutePath());
            return false;
        }
    }
}
