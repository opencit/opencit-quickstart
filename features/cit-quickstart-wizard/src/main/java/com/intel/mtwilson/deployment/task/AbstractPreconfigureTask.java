/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.task;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.deployment.OrderAware;
import com.intel.mtwilson.deployment.SoftwarePackage;
import com.intel.mtwilson.deployment.SoftwarePackageRepository;
import com.intel.mtwilson.deployment.SoftwarePackageRepositoryAware;
import com.intel.mtwilson.deployment.TargetAware;
import com.intel.mtwilson.deployment.descriptor.Target;
import com.intel.mtwilson.deployment.jaxrs.faults.FileNotCreated;
import com.intel.mtwilson.deployment.jaxrs.faults.FileNotFound;
import com.intel.mtwilson.deployment.jaxrs.io.OrderDocument;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.stringtemplate.v4.ST;

/**
 * 
 * @author jbuhacoff
 */
public abstract class AbstractPreconfigureTask extends AbstractTaskWithId implements OrderAware, TargetAware, SoftwarePackageRepositoryAware {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractPreconfigureTask.class);

    /**
     * The software repository where additional information about a software
     * package can be obtained. This object will be provided via 
     * setSoftwarePackageRepository(...) BEFORE calling execute().
     */
    protected SoftwarePackageRepository softwarePackageRepository;
    /**
     * The complete order document to provide additional needed context.
     * This object will be provided via setOrderDocument(...) BEFORE
     * calling execute()
     */
    protected OrderDocument order;
    /**
     * The host where the software should be pre-configured. This object
     * will provided via setTarget(...) BEFORE calling execute()
     */
    protected Target target;
    
    /**
     * A map of all configuration data generated or collected by this task, 
     * which is used when rendering templates
     */
    protected HashMap<String, Object> data = new HashMap<>();
    /**
     * A directory where the task can store temporary files for the order.
     * This is initialized in the constructor.
     */
    protected File taskDirectory;
    
    public AbstractPreconfigureTask() {
        super();
        taskDirectory = new File(Folders.repository("tasks") + File.separator + getId());
    }
    
    
    /**
     * Subclasses are required to implement this method which returns
     * the name of the package they know how to pre-configure; for
     * example "attestation_service" is the package name for Attestation Service
     * @return 
     */
    public abstract String getPackageName();
    
    /**
     * Usage example:
     * <pre>
     * File mtwilsonEnvFile = render("mtwilson.env.st4", "mtwilson.env");
     * </pre>
     * 
     * @param templateFileName like "mtwilson.env.st4" which is found in the software package directory NEXT TO the installer
     * @param outputFile like "mtwilson.env" which will be written to disk (should be in the task output directory)
     * @return a File reference to the outputFileName 
     * @throws RuntimeException and adds Faults to the task if any part of this process fails
     */
    protected void render(String templateFileName, File outputFile) {
        // read the template file        
        SoftwarePackage softwarePackage = softwarePackageRepository.searchByNameEquals(getPackageName());
        File installer = softwarePackage.getFile();
        File envFileTemplate = installer.toPath().resolveSibling(templateFileName).toFile();
        if (!envFileTemplate.exists() || !envFileTemplate.canRead()) {
            fault(new FileNotFound(envFileTemplate.getName()));
            throw new IllegalStateException();
        }
        //File outputFile = new File(taskDirectory.getAbsolutePath() + File.separator + outputFileName);
        try {
            String templateSource = FileUtils.readFileToString(envFileTemplate, "UTF-8");
            ST template = new ST(templateSource, '<', '>');
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                template.add(entry.getKey(), entry.getValue());
            }
            String output = template.render();
            outputFile.getParentFile().mkdirs();
            FileUtils.writeStringToFile(outputFile, output, "UTF-8");
        } catch (IOException e) {
            log.error("Cannot generate pre-configuration file: {}", outputFile.getAbsolutePath(), e);
            fault(new FileNotCreated(outputFile.getName()));
            throw new IllegalStateException();
        }
    }

    @Override
    public void setOrderDocument(OrderDocument order) {
        this.order = order;
    }

    @Override
    public void setTarget(Target target) {
        this.target = target;
    }

    @Override
    public void setSoftwarePackageRepository(SoftwarePackageRepository softwarePackageRepository) {
        this.softwarePackageRepository = softwarePackageRepository;
    }
    

    /**
     * Returns configuration data generated by this task; this should not
     * be called before execute() 
     * @return 
     */
    public HashMap<String, Object> getData() {
        return data;
    }
    
    public String getHost() {
        return target.getHost();
    }
}
