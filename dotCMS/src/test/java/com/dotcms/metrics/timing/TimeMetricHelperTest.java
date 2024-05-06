package com.dotcms.metrics.timing;

import com.dotcms.UnitTestBase;
import org.junit.Before;
import org.junit.Test;

import static graphql.Assert.assertTrue;

/**
 * Unit tests for the {@link TimeMetricHelper} class.
 *
 * @author vico
 */
public class TimeMetricHelperTest extends UnitTestBase {

    private TimeMetricHelper timeMetricHelper;

    @Before
    public void setUp() {
        timeMetricHelper = TimeMetricHelper.get();
    }

    /**
     * Test the {@link TimeMetricHelper#formatDuration(TimeMetric, String)} method with a custom mask.
     */
    @Test
    public void testFormatSecondsWithCustomMask() throws InterruptedException {
        final TimeMetric timeMetric = TimeMetric.mark("some-time-metric");
        Thread.sleep(100);
        timeMetric.stop();
        final String formatted = timeMetricHelper.formatDuration(timeMetric, "%.2f");
        assertTrue(formatted.startsWith("0.1"));
    }

    /**
     * Test the {@link TimeMetricHelper#formatDuration(TimeMetric)} method with the default mask.
     */
    @Test
    public void testFormatSecondsWithDefaultMask() throws InterruptedException {
        final TimeMetric timeMetric = TimeMetric.mark("some-time-metric");
        Thread.sleep(100);
        timeMetric.stop();
        final String formatted = timeMetricHelper.formatDuration(timeMetric);
        assertTrue(formatted.startsWith("0.10"));
    }

}
