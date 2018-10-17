/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.mtwilson.util.task.Task;
import com.intel.mtwilson.util.task.faults.PreconditionFailed;
import com.intel.mtwilson.util.task.faults.PostconditionFailed;
import com.intel.dcsg.cpg.configuration.Configurable;
import com.intel.dcsg.cpg.configuration.Configuration;
import com.intel.dcsg.cpg.configuration.PropertiesConfiguration;
import com.intel.dcsg.cpg.performance.Progress;
import com.intel.dcsg.cpg.validation.Fault;
import com.intel.mtwilson.util.task.faults.ExecutionFailed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A task can be in one of the following states: 
 * 
 * PENDING - task has not yet started (active is false and done is false)
 * 
 * PRECONDITIONS - task is evaluating preconditions, but is not yet running
 * (active is false, done is false)
 * 
 * ACTIVE - task is currently executing; progress
 * may be updated (done is false)
 * 
 * POSTCONDITIONS - task is evaluating postconditions, and is no longer running
 * (active is false, done is false)
 * 
 * DONE - task completed execution (active is false); this does not imply
 * if there are errors or not;  errors must be checked via getFaults()
 * 
 * @author jbuhacoff
 */
public abstract class AbstractTask implements Task, Progress, Preconditions, Postconditions, Configurable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractTask.class);
    private final ArrayList<Fault> faults;
    private final ArrayList<Task> dependencies;
    private final ArrayList<Condition> preconditions, postconditions;
    private Configuration configuration;
    private long current, max;
    private boolean active, done;

    public AbstractTask() {
        faults = new ArrayList<>();
        preconditions = new ArrayList<>();
        postconditions = new ArrayList<>();
        dependencies = new ArrayList<>();
        configuration = new PropertiesConfiguration();
        active = false;
        done = false;
    }

    @Override
    public final void run() {
        // reset.  subclasses can reset at beginning of execute()
        faults.clear();
        active = false;
        done = false;
        current = 0;
        max = 1;

        if( !testPreconditions() ) {
            log.debug("Aborting task due to failed preconditions");
            return;
        }

        active = true;
        
        try {
            execute();
        } catch (RuntimeException e) {
            log.error("Execution failed", e);
            faults.add(new ExecutionFailed(e));
        } 
        
        active = false;
        if (max == 1 && current == 0) {
            current = 1;
        }        
        
        if( !testPostconditions() ) {
            log.debug("Unfinished task due to failed postconditions");
            return;
        }

        // done is only on successful completion
        if( faults.isEmpty() ) {
            done = true;
        }
    }

    abstract public void execute();

    protected void fault(Fault fault) {
        faults.add(fault);
    }

    @Override
    public Collection<Fault> getFaults() {
        return faults;
    }

    private boolean testPreconditions() {
        boolean ok = true;
        for (Condition precondition : preconditions) {
            try {
                if (!precondition.test()) {
                    log.debug("Precondition failed: {}", precondition.getClass().getName());
                    fault(new PreconditionFailed(precondition));
                    ok = false;
                }
            } catch (RuntimeException e) {
                log.error("Precondition test exception", e);
                fault(new PreconditionFailed(precondition));
                ok = false;
            }
        }
        return ok;
    }

    private boolean testPostconditions() {
        boolean ok = true;
        for (Condition postcondition : postconditions) {
            try {
                if (!postcondition.test()) {
                    log.debug("Postcondition failed: {}", postcondition.getClass().getName());
                    fault(new PostconditionFailed(postcondition));
                    ok = false;
                }

            } catch (RuntimeException e) {
                log.error("Postcondition test exception", e);
                fault(new PostconditionFailed(postcondition));
                ok = false;
            }
        }
        return ok;
    }

    @Override
    public Collection<Condition> getPostconditions() {
        return Collections.unmodifiableCollection(postconditions);
    }

    @Override
    public Collection<Condition> getPreconditions() {
        return Collections.unmodifiableCollection(preconditions);
    }

    protected void precondition(Condition precondition) {
        preconditions.add(precondition);
    }

    protected void postcondition(Condition condition) {
        postconditions.add(condition);
    }

    @Override
    public void configure(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public ArrayList<Task> getDependencies() {
        return dependencies;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    protected void max(long max) {
        this.max = max;
    }

    protected void current(long current) {
        this.current = current;
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
