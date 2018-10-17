/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.dcsg.cpg.performance.Observer;
import com.intel.dcsg.cpg.performance.Progress;
import com.intel.dcsg.cpg.performance.ProgressMonitor;
import com.intel.dcsg.cpg.performance.ProgressMonitor.ProgressLogObserver;

/**
 *
 * @author jbuhacoff
 */
public class ProgressMonitoringTask extends AbstractTaskDecorator {

    private Observer<Progress> observer;

    public ProgressMonitoringTask(Task delegate) {
        super(delegate);
        this.observer = new ProgressLogObserver();
    }

    public ProgressMonitoringTask(Task delegate, Observer<Progress> observer) {
        super(delegate);
        this.observer = observer;
    }

    @Override
    public void run() {
        ProgressCounter counter = null;
        Progress report;
        if (delegate instanceof Progress) {
            report = (Progress) delegate;
        } else {
            report = counter = new ProgressCounter(0, 1);
        }
        ProgressMonitor monitor = new ProgressMonitor(report, observer);
        monitor.start();
        delegate.run();
        if (counter != null) {
            counter.increment();
        }
        monitor.stop();
    }

    /**
     * Used when monitoring a task that doesn't implement Progress. Starts with
     * current=0,max=1 and reports current=1,max=1 when task is done.
     */
    public static class ProgressCounter implements Progress {

        private long current, max;

        public ProgressCounter(long current, long max) {
            this.current = current;
            this.max = max;
        }

        public void increment() {
            current++;
        }

        @Override
        public long getCurrent() {
            return current;
        }

        @Override
        public long getMax() {
            return max;
        }
    }
}
