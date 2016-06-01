/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.jaxrs;

import com.intel.mtwilson.deployment.jaxrs.io.SoftwarePackageDocument;
import com.intel.mtwilson.deployment.jaxrs.io.SoftwarePackageDocumentCollection;
import com.intel.mtwilson.deployment.jaxrs.io.SoftwarePackageDocumentRepository;
import com.intel.mtwilson.deployment.jaxrs.io.SoftwarePackageFilterCriteria;
import com.intel.mtwilson.deployment.jaxrs.io.SoftwarePackageLocator;
import com.intel.mtwilson.jaxrs2.NoLinks;
import com.intel.mtwilson.jaxrs2.Patch;
import com.intel.mtwilson.jaxrs2.server.resource.AbstractJsonapiResource;
import com.intel.mtwilson.launcher.ws.ext.V2;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

/**
 *
 * @author jbuhacoff
 */
@V2
@Path("/quickstart/packages")
public class Packages extends AbstractJsonapiResource<SoftwarePackageDocument, SoftwarePackageDocumentCollection, SoftwarePackageFilterCriteria, NoLinks<SoftwarePackageDocument>, SoftwarePackageLocator> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Packages.class);
    private SoftwarePackageDocumentRepository repository;
    
    public Packages() {
        repository = new SoftwarePackageDocumentRepository();
    }
    
    @Override
    protected SoftwarePackageDocumentCollection createEmptyCollection() {
        return new SoftwarePackageDocumentCollection();
    }

    @Override
    protected SoftwarePackageDocumentRepository getRepository() {
        return repository;
    }
    
        

    // refuse to let client store or patch orders arbitrarily;  server controls updates to orders
    
    @Override
    public SoftwarePackageDocument createOne(@BeanParam SoftwarePackageLocator locator, SoftwarePackageDocument item, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public SoftwarePackageDocumentCollection storeJsonapiCollection(@BeanParam SoftwarePackageLocator locator, SoftwarePackageDocumentCollection collection) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public SoftwarePackageDocument storeOne(@BeanParam SoftwarePackageLocator locator, SoftwarePackageDocument item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SoftwarePackageDocumentCollection patchJsonapiCollection(@BeanParam SoftwarePackageLocator locator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SoftwarePackageDocument patchOne(@BeanParam SoftwarePackageLocator locator, Patch<SoftwarePackageDocument, SoftwarePackageFilterCriteria, NoLinks<SoftwarePackageDocument>>[] patchArray) {
        throw new UnsupportedOperationException();
    }

    
}
