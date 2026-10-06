package com.dotcms.metrics.timing;

import com.dotcms.UnitTestBase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link TimeMetricHelper} class.
 *
 * <p>The duration is stubbed rather than produced by a {@code Thread.sleep()}. Asserting on a
 * slept-through duration made these tests depend on sleep precision: the default {@code %.4f} mask
 * only tolerated a ~10ms overshoot, which fails on any loaded machine.</p>
 *
 * @author vico
 */
public class TimeMetricHelperTest extends UnitTestBase {

    private static final long DURATION_MS = 1500L;
    private static final float DURATION_SECONDS = 1.5f;

    private TimeMetricHelper timeMetricHelper;

    @Before
    public void setUp() {
        timeMetricHelper = TimeMetricHelper.get();
    }

    /**
     * Test the {@link TimeMetricHelper#formatDuration(TimeMetric, String)} method with a custom mask.
     */
    @Test
    public void testFormatSecondsWithCustomMask() {
        final TimeMetric timeMetric = mock(TimeMetric.class);
        when(timeMetric.getDuration()).thenReturn(DURATION_MS);

        assertEquals(String.format("%.2f", DURATION_SECONDS),
                timeMetricHelper.formatDuration(timeMetric, "%.2f"));
    }

    /**
     * Test the {@link TimeMetricHelper#formatDuration(TimeMetric)} method with the default mask.
     */
    @Test
    public void testFormatSecondsWithDefaultMask() {
        final TimeMetric timeMetric = mock(TimeMetric.class);
        when(timeMetric.getDuration()).thenReturn(DURATION_MS);

        assertEquals(String.format("%.4f", DURATION_SECONDS),
                timeMetricHelper.formatDuration(timeMetric));
    }

}
