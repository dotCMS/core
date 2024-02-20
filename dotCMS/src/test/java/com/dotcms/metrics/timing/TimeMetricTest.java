package com.dotcms.metrics.timing;

import com.dotcms.UnitTestBase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link TimeMetric} class.
 */
public class TimeMetricTest extends UnitTestBase {

    private TimeMetric timeMetric;

    /**
     * Set up the test environment by initializing the {@link TimeMetric} instance.
     */
    @Before
    public void setUp() {
        timeMetric = TimeMetric.mark("TestMetric");
    }

    /**
     * Test the stop() method of the {@link TimeMetric} class.
     */
    @Test
    public void testStartAndStop() {
        assertNotNull("TimeMetric instance should not be null", timeMetric);
        timeMetric.start();
        assertTrue("Timer should be stopped after calling stop()", timeMetric.stop().getDuration() >= 0);
    }

}

