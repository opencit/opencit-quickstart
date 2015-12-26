/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.descriptor.SoftwarePackageDescriptor;
import com.intel.mtwilson.deployment.descriptor.SoftwarePackageDescriptorCollection;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jbuhacoff
 */
public class JsonSoftwarePackageRepository implements SoftwarePackageRepository {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonSoftwarePackageRepository.class);
    private final Map<String,SoftwarePackage> softwarePackagesMap = new HashMap<>();
    private final ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();

    public JsonSoftwarePackageRepository(InputStream in) throws IOException {
        List<SoftwarePackage> list = read(in);
        for(SoftwarePackage item : list) {
            softwarePackagesMap.put(item.getPackageName(), item);
        }
    }

    private List<SoftwarePackage> read(InputStream in) throws IOException {
        SoftwarePackageDescriptorCollection descriptorCollection = mapper.readValue(in, SoftwarePackageDescriptorCollection.class);
        assert descriptorCollection.getSoftwarePackages() != null;
        ArrayList<SoftwarePackage> softwarePackageList = new ArrayList<>();

        // first convert each descriptor to a software package instance with package name and filename
        for (SoftwarePackageDescriptor descriptor : descriptorCollection.getSoftwarePackages()) {
            File file = getPackageFile(descriptor.getPackageName(), descriptor.getFileName());
            SoftwarePackage softwarePackage = new SoftwarePackage(descriptor.getPackageName(), file);
            softwarePackageList.add(softwarePackage);
        }
        // second, make a map of package name => software package instance
        HashMap<String, SoftwarePackage> softwarePackageMap = new HashMap<>();
        for (SoftwarePackage softwarePackage : softwarePackageList) {
            softwarePackageMap.put(softwarePackage.getPackageName(), softwarePackage);
        }
        // third, for each package, find the dependencies in the map and associate them
        for (SoftwarePackageDescriptor descriptor : descriptorCollection.getSoftwarePackages()) {
            Collection<String> dependencyNames = descriptor.getDependencies();
            SoftwarePackage subject = softwarePackageMap.get(descriptor.getPackageName());
            for (String dependencyName : dependencyNames) {
                SoftwarePackage dependency = softwarePackageMap.get(dependencyName);
                if (dependency != null) {
                    subject.getDependencies().add(dependency);
                } else {
                    log.warn("Cannot resolve dependency: {}", dependencyName);
                }
            }
        }

        return softwarePackageList;
    }
    
    private File getPackageFile(String packageName, String filename) throws FileNotFoundException, IOException {
        if (filename == null || filename.isEmpty()) {
            return null; // throw new FileNotFoundException(packageName);
        }
        File file = new File(Folders.repository("packages") + File.separator + packageName + File.separator + filename);

        // ensure that file is actually in our repository - to protect localhost we will not
        // copy to a remote host any file that is outside our package repository
        if (!file.getAbsolutePath().startsWith(Folders.repository("packages") + File.separator)) {
            log.error("Attempt to reference file outside package repository: {}", file.getAbsolutePath());
            return null; // throw new FileNotFoundException(packageName);
        }

        if (!file.exists()) {
            log.error("File does not exist: {}", file.getAbsolutePath());
            return null; // throw new FileNotFoundException(packageName);
        }

        return file;
    }
    
    
    @Override
    public SoftwarePackage searchByNameEquals(String name) {
        return softwarePackagesMap.get(name);
    }

    @Override
    public List<SoftwarePackage> listAll() {
        ArrayList<SoftwarePackage> list = new ArrayList<>();
        list.addAll(softwarePackagesMap.values());
        return list;
    }

}

