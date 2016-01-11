/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.deployment.threads;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;

/**
 *
 * @author jbuhacoff
 */
public class QuartzManager implements ServletContextListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QuartzManager.class);
    private Scheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
        try {
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();
        } catch (Exception e) {
            log.error("Cannot initialize quartz scheduler", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (Exception e) {
                log.error("Cannot shutdown quartz scheduler", e);
            }
        }
    }
}
