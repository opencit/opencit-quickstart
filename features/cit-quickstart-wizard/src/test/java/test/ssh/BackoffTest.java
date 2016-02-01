/*
 * Copyright (C) 2015 Intel Corporation
 * All rights reserved.
 */
package test.ssh;

import com.intel.mtwilson.deployment.retry.Backoff;
import com.intel.mtwilson.deployment.retry.ConstantBackoff;
import com.intel.mtwilson.deployment.retry.ExponentialBackoff;
import com.intel.mtwilson.deployment.retry.LimitedBackoff;
import com.intel.mtwilson.deployment.retry.NearestBackoff;
import com.intel.mtwilson.deployment.retry.RandomBackoff;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jbuhacoff
 */
public class BackoffTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BackoffTest.class);

    /**
     * this is a trivial example to compare with other backoff implementations
     */
    @Test
    public void testConstantBackoff() {
        long expected = 50;
        Backoff backoff = new ConstantBackoff(expected);
        for(int i=0; i<10; i++) {
            long result = backoff.getMilliseconds();
            log.debug("constant backoff: {}ms", result);
            assertEquals(expected, result);
        }
    }


    @Test
    public void testRandomBackoff() {
        long min = 10, max = 100;
        Backoff backoff = new RandomBackoff(min, max);
        for(int i=0; i<100; i++) {
            long result = backoff.getMilliseconds();
            log.debug("random backoff: {}ms", result);
            assertTrue( min <= result && result <= max );
        }
    }

    @Test
    public void testNearestBackoff() {
        long min = 5, max = 322;
        Backoff backoff = new NearestBackoff(25, new RandomBackoff(min, max));
        for(int i=0; i<100; i++) {
            long result = backoff.getMilliseconds();
            log.debug("random backoff: {}ms", result);
            assertEquals(0, result % 25 );
        }
    }
    
    @Test
    public void testExponentialBackoff() {
        long min=50, max = 1024;
//        Backoff backoff = new ExponentialBackoff(2, max);
        Backoff backoff = new LimitedBackoff(min, max, new NearestBackoff(25, new ExponentialBackoff(max))); // min interval, backoff in 100ms increments up to max

        for(int i=0; i<100; i++) {
            long result = backoff.getMilliseconds();
            log.debug("exponential backoff: {}ms", result);
            assertTrue( min <= result && result <= max );
        }
    }
    
}

