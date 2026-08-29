package com.dotcms.job.system.event;

import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SystemEventsConfig}, the configuration surface of the system event delivery
 * cursor (issue #36827).
 *
 * <p>The behaviour pinned here that is easy to get wrong: the backlog clamp must stay well below the
 * retention window of {@code DeleteOldSystemEventsJob}. If it does not, the clamp can point a
 * recovering node at rows the purge job has already deleted, and the node silently believes it has
 * caught up.
 */
public class SystemEventsConfigTest {

    private String originalOverlap;
    private String originalBacklog;
    private String originalLagThreshold;
    private String originalRetention;

    @Before
    public void setUp() {
        originalOverlap = Config.getStringProperty(SystemEventsConfig.OVERLAP_WINDOW_SECONDS, null);
        originalBacklog = Config.getStringProperty(SystemEventsConfig.MAX_BACKLOG_MINUTES, null);
        originalLagThreshold =
                Config.getStringProperty(SystemEventsConfig.LAG_WARN_THRESHOLD_PERCENT, null);
        originalRetention = Config.getStringProperty(SystemEventsConfig.DELETE_EVENTS_OLDER_THAN, null);
    }

    @After
    public void tearDown() {
        restore(SystemEventsConfig.OVERLAP_WINDOW_SECONDS, originalOverlap);
        restore(SystemEventsConfig.MAX_BACKLOG_MINUTES, originalBacklog);
        restore(SystemEventsConfig.LAG_WARN_THRESHOLD_PERCENT, originalLagThreshold);
        restore(SystemEventsConfig.DELETE_EVENTS_OLDER_THAN, originalRetention);
    }

    private void restore(final String key, final String value) {
        if (null == value) {
            Config.setProperty(key, null);
        } else {
            Config.setProperty(key, value);
        }
    }

    /**
     * Method to test: the documented defaults of {@link SystemEventsConfig}
     * Given Scenario: No properties are overridden
     * ExpectedResult: The defaults from data-model.md apply — 120s overlap, 60min backlog, 50% lag
     */
    @Test
    public void test_defaults_match_the_documented_values() {
        Config.setProperty(SystemEventsConfig.OVERLAP_WINDOW_SECONDS, null);
        Config.setProperty(SystemEventsConfig.MAX_BACKLOG_MINUTES, null);
        Config.setProperty(SystemEventsConfig.LAG_WARN_THRESHOLD_PERCENT, null);

        assertEquals(120, SystemEventsConfig.getOverlapWindowSeconds());
        assertEquals(TimeUnit.SECONDS.toMillis(120), SystemEventsConfig.getOverlapWindowMillis());
        assertEquals(60, SystemEventsConfig.getMaxBacklogMinutes());
        assertEquals(TimeUnit.MINUTES.toMillis(60), SystemEventsConfig.getMaxBacklogMillis());
        assertEquals(50, SystemEventsConfig.getLagWarnThresholdPercent());
    }

    /**
     * Method to test: {@link SystemEventsConfig#isBacklogWithinRetention()}
     * Given Scenario: The backlog clamp (60 minutes) sits well inside the 31-day retention window
     * ExpectedResult: The configuration is reported as consistent
     */
    @Test
    public void test_default_backlog_is_within_retention() {
        Config.setProperty(SystemEventsConfig.MAX_BACKLOG_MINUTES, null);
        Config.setProperty(SystemEventsConfig.DELETE_EVENTS_OLDER_THAN, null);

        assertTrue(SystemEventsConfig.isBacklogWithinRetention());
    }

    /**
     * Method to test: {@link SystemEventsConfig#isBacklogWithinRetention()}
     * Given Scenario: The backlog clamp is configured LONGER than the retention window, so the clamp
     * would point a recovering node at rows the purge job has already deleted
     * ExpectedResult: The configuration is reported as inconsistent
     */
    @Test
    public void test_backlog_longer_than_retention_is_rejected() {
        Config.setProperty(SystemEventsConfig.DELETE_EVENTS_OLDER_THAN, 1);          // 1 day
        Config.setProperty(SystemEventsConfig.MAX_BACKLOG_MINUTES, 60 * 24 * 2);     // 2 days

        assertFalse(SystemEventsConfig.isBacklogWithinRetention());
    }

    /**
     * Method to test: {@link SystemEventsConfig#isBacklogWithinRetention()}
     * Given Scenario: The backlog clamp equals the retention window exactly — no safety margin
     * ExpectedResult: Rejected. "Well below" means a margin, not equality; a clamp landing exactly on
     * the purge boundary races the purge job.
     */
    @Test
    public void test_backlog_equal_to_retention_is_rejected() {
        Config.setProperty(SystemEventsConfig.DELETE_EVENTS_OLDER_THAN, 1);          // 1 day
        Config.setProperty(SystemEventsConfig.MAX_BACKLOG_MINUTES, 60 * 24);         // 1 day

        assertFalse(SystemEventsConfig.isBacklogWithinRetention());
    }

    /**
     * Method to test: {@link SystemEventsConfig#getOverlapWindowSeconds()}
     * Given Scenario: A non-positive overlap window is configured, which would disable the re-read
     * that fixes the commit-timing race entirely
     * ExpectedResult: The value falls back to the default rather than silently disabling the fix
     */
    @Test
    public void test_non_positive_overlap_window_falls_back_to_the_default() {
        Config.setProperty(SystemEventsConfig.OVERLAP_WINDOW_SECONDS, 0);
        assertEquals(120, SystemEventsConfig.getOverlapWindowSeconds());

        Config.setProperty(SystemEventsConfig.OVERLAP_WINDOW_SECONDS, -5);
        assertEquals(120, SystemEventsConfig.getOverlapWindowSeconds());
    }
}
