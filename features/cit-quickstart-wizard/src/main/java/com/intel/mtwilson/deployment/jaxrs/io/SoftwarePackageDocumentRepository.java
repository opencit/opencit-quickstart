/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs.io;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.SoftwarePackageRepository;
import com.intel.mtwilson.deployment.SoftwarePackageRepositoryFactory;
import com.intel.mtwilson.jaxrs2.server.resource.DocumentRepository;
import com.intel.mtwilson.repository.RepositoryCreateConflictException;
import com.intel.mtwilson.repository.RepositoryCreateException;
import com.intel.mtwilson.repository.RepositoryRetrieveException;
import com.intel.mtwilson.repository.RepositorySearchException;
import com.intel.mtwilson.repository.RepositoryStoreException;
import com.intel.mtwilson.task.JsonFileRepository;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author jbuhacoff
 */
public class SoftwarePackageDocumentRepository implements DocumentRepository<SoftwarePackageDocument, SoftwarePackageDocumentCollection, SoftwarePackageFilterCriteria, SoftwarePackageLocator> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SoftwarePackageDocumentRepository.class);
    private JsonFileRepository json;

    public SoftwarePackageDocumentRepository() {
        super();
        File directory = new File(Folders.repository("orders"));
        try {
            json = new JsonFileRepository(directory);
        } catch (FileNotFoundException e) {
            log.error("Cannot create repository directory: {}", directory.getAbsolutePath(), e);
            json = null;
        }
    }

    /**
     * Currently supports searching only by order id.
     *
     * @param criteria
     * @return a SoftwarePackageCollection instance; may be empty if no matches found but
     * will never be null
     */
    @Override
    public SoftwarePackageDocumentCollection search(SoftwarePackageFilterCriteria criteria) {
        try {
            SoftwarePackageRepository repository = SoftwarePackageRepositoryFactory.getInstance();
            List<SoftwarePackage> list = repository.listAll();
            SoftwarePackageDocumentCollection collection = new SoftwarePackageDocumentCollection();
            for(SoftwarePackage item : list) {
                if( criteria.nameEqualTo == null || criteria.nameEqualTo.equals(item.getPackageName()) ) {
                    SoftwarePackageDocument document = new SoftwarePackageDocument();
                    document.setName(item.getPackageName());
                    document.setAvailableVariants(item.getAvailableVariants());
                    collection.getDocuments().add(document);
                }
            }
            return collection;
        }
        catch(Exception e) {
            throw new RepositorySearchException(e);
        }
    }

    /**
     * Searches for orders matching the specified criteria and deletes them.
     *
     * @param criteria
     */
    @Override
    public void delete(SoftwarePackageFilterCriteria criteria) {
        throw new RepositoryStoreException("Unsupported operation");
    }

    /**
     *
     * @param locator
     * @return order instance or null if order was not found
     */
    @Override
    public SoftwarePackageDocument retrieve(SoftwarePackageLocator locator) {
        throw new RepositoryStoreException("Unsupported operation");
    }

    @Override
    public void store(SoftwarePackageDocument item) {
        throw new RepositoryStoreException("Unsupported operation");
    }

    @Override
    public void create(SoftwarePackageDocument item) {
        throw new RepositoryStoreException("Unsupported operation");
    }

    @Override
    public void delete(SoftwarePackageLocator locator) {
        throw new RepositoryStoreException("Unsupported operation");
    }
}
