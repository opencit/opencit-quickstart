/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.threads;

import com.intel.dcsg.cpg.configuration.Configurable;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import java.io.IOException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 *
 * @author jbuhacoff
 */
@WebListener
public class QuartzManager implements ServletContextListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuartzManager.class);
    private static final String QUARTZ_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
    private Scheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("QuartzManager contextInitialized");
        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
        sce.getServletContext().setAttribute(QUARTZ_KEY, schedulerFactory);
        try {
            Configuration configuration = ConfigurationFactory.getConfiguration();
            long orderCleanupInterval = Long.valueOf(configuration.get("mtwilson.quickstart.order.cleanup.interval", "600000")).longValue(); // default run every 10 minutes
            
            JobDetail cleanupJob = JobBuilder.newJob(QuartzJobRunnableAdaptor.class)
                    .withIdentity("OrderCleanup")
                    .usingJobData("runnable.class", "com.intel.mtwilson.deployment.threads.OrderCleanup")
                    .build();
            
            Trigger cleanupJobTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("OrderCleanupTrigger")
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(orderCleanupInterval))
                    .startNow()
                    .build();
            
            scheduler = schedulerFactory.getScheduler();
            scheduler.scheduleJob(cleanupJob, cleanupJobTrigger);
            scheduler.start();
            log.debug("Started quartz scheduler");
        } catch (IOException | NumberFormatException | SchedulerException e) {
            log.error("Cannot initialize quartz scheduler", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("QuartzManager contextDestroyed");
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (Exception e) {
                log.error("Cannot shutdown quartz scheduler", e);
            }
        }
    }

    /**
     * Executes the run() method of any Runnable.
     * If initializing directly then
     * just pass the runnable instance to the constructor.
     * If initializing via Quartz then 
     * add a key "runnable.class" to the JobDataMap with the class name
     * to instantiate and run as the value. The class must implement
     * the Runnable interface. 
     * If the Runnable also implements Configurable, the application configuration
     * instance will be set prior to calling run().
     */
    public static class QuartzJobRunnableAdaptor implements Job {

        @Override
        public void execute(JobExecutionContext jec) throws JobExecutionException {
            String runnableClassName = jec.getJobDetail().getJobDataMap().getString("runnable.class");
            try {
                Runnable runnable = null;
                if(runnableClassName != null ) {
                    Class runnableClass = Class.forName(runnableClassName);
                    Object runnableObject = runnableClass.newInstance();
                    if( runnableObject instanceof Runnable ) {
                        runnable = (Runnable)runnableObject;
                    }
                    else {
                        log.error("Class {} does not implement Runnable", runnableClassName);
                        throw new IllegalArgumentException(runnableClassName);
                    }
                }
                if( runnable != null ) {
                    if (runnable instanceof Configurable) {
                        Configuration configuration = ConfigurationFactory.getConfiguration();
                        Configurable configurable = (Configurable) runnable;
                        configurable.configure(configuration);
                    }
                    runnable.run();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | IOException e) {
                log.error("Error while running job class {}", runnableClassName);
                throw new JobExecutionException(e);
            }
        }
    }
}
