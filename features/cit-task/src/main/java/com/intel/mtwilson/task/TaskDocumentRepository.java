/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.task;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.jaxrs2.server.resource.DocumentRepository;
import com.intel.mtwilson.repository.RepositoryCreateConflictException;
import com.intel.mtwilson.repository.RepositoryCreateException;
import com.intel.mtwilson.repository.RepositoryRetrieveException;
import com.intel.mtwilson.repository.RepositoryStoreException;
import com.intel.mtwilson.task.jaxrs.TaskDocumentCollection;
import com.intel.mtwilson.task.jaxrs.TaskFilterCriteria;
import com.intel.mtwilson.task.jaxrs.TaskLocator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class TaskDocumentRepository implements DocumentRepository<TaskDocument, TaskDocumentCollection, TaskFilterCriteria, TaskLocator> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskDocumentRepository.class);
    private JsonFileRepository json;

    public TaskDocumentRepository() {
        super();
        File directory = new File(Folders.repository("tasks"));
        try {
            json = new JsonFileRepository(directory);
        } catch (FileNotFoundException e) {
            log.error("Cannot create repository directory: {}", directory.getAbsolutePath(), e);
            json = null;
        }
    }

    /**
     * Currently supports searching only by task id.
     *
     * @param criteria
     * @return a TaskCollection instance; may be empty if no matches found but
     * will never be null
     */
    @Override
    public TaskDocumentCollection search(TaskFilterCriteria criteria) {
        TaskDocumentCollection results = new TaskDocumentCollection();
        if (criteria != null && criteria.id != null) {
            try {
                TaskDocument found = json.retrieve(criteria.id.toString(), TaskDocument.class);
                if (found != null) {
                    results.getTasks().add(found);
                    return results;
                }
            } catch (Exception e) {
                log.error("Cannot retrieve task: {}", criteria.id.toString(), e);
            }
        }
        else if (criteria != null && !criteria.filter) {
            List<String> taskIds = json.list();
            for(String taskId : taskIds) {
                try {
                TaskDocument found = json.retrieve(taskId, TaskDocument.class);
                if (found != null) {
                    results.getTasks().add(found);
                }
            } catch (Exception e) {
                log.error("Cannot retrieve task: {}", taskId, e);
            }
               
                
            }
        }
        return results;
    }

    /**
     * Searches for tasks matching the specified criteria and deletes them.
     *
     * @param criteria
     */
    @Override
    public void delete(TaskFilterCriteria criteria) {
        TaskDocumentCollection tasksToDelete = search(criteria);
        for (TaskDocument taskToDelete : tasksToDelete.getTasks()) {
            json.remove(taskToDelete.getId().toString());
        }
    }

    /**
     *
     * @param locator
     * @return task instance or null if task was not found
     */
    @Override
    public TaskDocument retrieve(TaskLocator locator) {
        if (locator == null || locator.id == null || !json.contains(locator.id.toString())) {
            return null;
        }
        try {
            return json.retrieve(locator.id.toString(), TaskDocument.class);
        } catch (IOException e) {
            log.error("Cannot retrieve task: {}", locator.id.toString(), e);
            throw new RepositoryRetrieveException(e);
        }
    }

    @Override
    public void store(TaskDocument item) {
        if (item == null || item.getId() == null) {
            throw new RepositoryStoreException();
        }
        try {
            json.store(item);
        } catch (IOException e) {
            log.error("Cannot store task: {}", item.getId().toString(), e);
            throw new RepositoryStoreException(e);
        }
    }

    @Override
    public void create(TaskDocument item) {
        if (item == null || item.getId() == null) {
            throw new RepositoryCreateException();
        }
        if (json.contains(item.getId().toString())) {
            throw new RepositoryCreateConflictException();
        }
        try {
            json.create(item);
        } catch (IOException e) {
            log.error("Cannot store task: {}", item.getId().toString(), e);
            throw new RepositoryCreateException(e);
        }
    }

    @Override
    public void delete(TaskLocator locator) {
        TaskDocument taskToDelete = retrieve(locator);
        if (taskToDelete != null) {
            UUID uuid = taskToDelete.getId();
            json.remove(uuid.toString());
        }
    }
}
