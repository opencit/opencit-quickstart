/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.util.task;

import com.intel.dcsg.cpg.performance.AlarmClock;
import com.intel.dcsg.cpg.performance.report.PerformanceInfo;
import java.util.ArrayList;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

/**
 *
 * @author jbuhacoff
 */
public class ClockingTaskTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClockingTaskTest.class);

    @Test
    public void testClockingTask() {
        int iterations = 10;
        ArrayList<Long> elapsedTimes = new ArrayList<>();
        TaskRunClockListener clockingTask = new TaskRunClockListener();
        TaskRunListenerDecorator decorator = new TaskRunListenerDecorator(new Delay());
        decorator.getListeners().add(clockingTask);
        for(int i=0; i<iterations; i++) {
            decorator.run();
            elapsedTimes.add( clockingTask.getTimeStopped() - clockingTask.getTimeStarted() );
        }
        PerformanceInfo info = new PerformanceInfo(elapsedTimes);
        log.debug("min {}ms max {}ms avg {}ms over {} iterations", info.getMin(), info.getMax(), info.getAverage(), iterations);
    }
    
    public static class Delay extends AbstractTask {

        @Override
        public void execute() {
            AlarmClock alarm = new AlarmClock();
            alarm.sleep(RandomUtils.nextInt(1000));
        }
        
    }
}
