package com.dotcms.job.system.event;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Configuration surface for the system event delivery cursor (issue #36827).
 *
 * <p>Two of these settings are correctness-relevant rather than merely tuning:
 *
 * <ul>
 *   <li>The <b>overlap window</b> is how far back each poll re-reads. It is what makes an event that
 *       commits after its {@code created} timestamp still eligible for delivery, so a non-positive
 *       value would silently disable the fix. Such a value is rejected in favour of the default
 *       rather than honoured.</li>
 *   <li>The <b>backlog clamp</b> bounds how far a returning node reaches back. It must stay well
 *       below the retention window of {@code DeleteOldSystemEventsJob}, or the clamp can point a
 *       recovering node at rows the purge job has already deleted — and the node would believe it
 *       had caught up.</li>
 * </ul>
 */
public class SystemEventsConfig {

    /** How far back each poll re-reads, so an event committing late is still caught. */
    public static final String OVERLAP_WINDOW_SECONDS = "SYSTEM_EVENTS_OVERLAP_WINDOW_SECONDS";

    /** Clamp for a stale persisted cursor, bounding recovery after node downtime. */
    public static final String MAX_BACKLOG_MINUTES = "SYSTEM_EVENTS_MAX_BACKLOG_MINUTES";

    /** Share of the overlap window at which observed commit lag raises a warning. */
    public static final String LAG_WARN_THRESHOLD_PERCENT =
            "SYSTEM_EVENTS_LAG_WARN_THRESHOLD_PERCENT";

    /** Existing retention property owned by {@code DeleteOldSystemEventsDelegate}, in days. */
    public static final String DELETE_EVENTS_OLDER_THAN = "DELETE_EVENTS_OLDER_THAN";

    /** How often the authored-vs-observed reconciliation runs, in minutes. */
    public static final String RECONCILE_INTERVAL_MINUTES = "SYSTEM_EVENTS_RECONCILE_INTERVAL_MINUTES";

    /** The poll cadence, used only to judge whether the cursor has gone stale. */
    public static final String POLL_INTERVAL_SECONDS = "SYSTEM_EVENTS_POLL_INTERVAL_SECONDS";

    static final int DEFAULT_OVERLAP_WINDOW_SECONDS = 120;
    static final int DEFAULT_MAX_BACKLOG_MINUTES = 60;
    static final int DEFAULT_LAG_WARN_THRESHOLD_PERCENT = 50;
    static final int DEFAULT_DELETE_EVENTS_OLDER_THAN_DAYS = 31;
    static final int DEFAULT_RECONCILE_INTERVAL_MINUTES = 60;
    /** Matches the default SYSTEM_EVENTS_CRON_EXPRESSION of {@code 0/5 * * * * ?}. */
    static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;

    /**
     * "Well below" retention means a real margin, not merely "not greater than". A backlog clamp
     * landing on the purge boundary races {@code DeleteOldSystemEventsJob}, so the clamp is required
     * to sit within this fraction of the retention window.
     */
    static final int BACKLOG_RETENTION_SAFETY_FACTOR = 2;

    private SystemEventsConfig() {
    }

    /**
     * @return the overlap window in seconds; a non-positive configured value falls back to the
     * default, because honouring it would disable the late-commit re-read entirely
     */
    public static int getOverlapWindowSeconds() {
        return positiveOrDefault(
                Config.getIntProperty(OVERLAP_WINDOW_SECONDS, DEFAULT_OVERLAP_WINDOW_SECONDS),
                DEFAULT_OVERLAP_WINDOW_SECONDS, OVERLAP_WINDOW_SECONDS);
    }

    /**
     * @return the overlap window in milliseconds
     */
    public static long getOverlapWindowMillis() {
        return TimeUnit.SECONDS.toMillis(getOverlapWindowSeconds());
    }

    /**
     * @return the backlog clamp in minutes; a non-positive configured value falls back to the default
     */
    public static int getMaxBacklogMinutes() {
        return positiveOrDefault(
                Config.getIntProperty(MAX_BACKLOG_MINUTES, DEFAULT_MAX_BACKLOG_MINUTES),
                DEFAULT_MAX_BACKLOG_MINUTES, MAX_BACKLOG_MINUTES);
    }

    /**
     * @return the backlog clamp in milliseconds
     */
    public static long getMaxBacklogMillis() {
        return TimeUnit.MINUTES.toMillis(getMaxBacklogMinutes());
    }

    /**
     * @return the percentage of the overlap window at which a commit-lag warning is raised
     */
    public static int getLagWarnThresholdPercent() {
        return positiveOrDefault(
                Config.getIntProperty(LAG_WARN_THRESHOLD_PERCENT,
                        DEFAULT_LAG_WARN_THRESHOLD_PERCENT),
                DEFAULT_LAG_WARN_THRESHOLD_PERCENT, LAG_WARN_THRESHOLD_PERCENT);
    }

    /**
     * @return the retention window of {@code DeleteOldSystemEventsJob}, in days
     */
    public static int getRetentionDays() {
        return positiveOrDefault(
                Config.getIntProperty(DELETE_EVENTS_OLDER_THAN,
                        DEFAULT_DELETE_EVENTS_OLDER_THAN_DAYS),
                DEFAULT_DELETE_EVENTS_OLDER_THAN_DAYS, DELETE_EVENTS_OLDER_THAN);
    }

    /**
     * @return how often reconciliation runs, in milliseconds
     */
    public static long getReconcileIntervalMillis() {
        return TimeUnit.MINUTES.toMillis(positiveOrDefault(
                Config.getIntProperty(RECONCILE_INTERVAL_MINUTES, DEFAULT_RECONCILE_INTERVAL_MINUTES),
                DEFAULT_RECONCILE_INTERVAL_MINUTES, RECONCILE_INTERVAL_MINUTES));
    }

    /**
     * @return the poll cadence in milliseconds, used to judge cursor staleness
     */
    public static long getPollIntervalMillis() {
        return TimeUnit.SECONDS.toMillis(positiveOrDefault(
                Config.getIntProperty(POLL_INTERVAL_SECONDS, DEFAULT_POLL_INTERVAL_SECONDS),
                DEFAULT_POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS));
    }

    /**
     * Checks that the backlog clamp sits well inside the retention window. Equality is rejected: a
     * clamp landing exactly on the purge boundary races the purge job.
     *
     * @return true when the configuration is consistent
     */
    public static boolean isBacklogWithinRetention() {
        final long backlogMillis = getMaxBacklogMillis();
        final long retentionMillis = TimeUnit.DAYS.toMillis(getRetentionDays());
        return backlogMillis <= (retentionMillis / BACKLOG_RETENTION_SAFETY_FACTOR);
    }

    /**
     * Validates the configuration and logs a warning when the backlog clamp is not safely inside the
     * retention window. Called at poller startup so a misconfiguration is visible rather than
     * silently producing a cursor that points at purged rows.
     *
     * @return true when the configuration is consistent
     */
    public static boolean validateConfiguration() {
        if (!isBacklogWithinRetention()) {
            Logger.warn(SystemEventsConfig.class,
                    "System event configuration is inconsistent: " + MAX_BACKLOG_MINUTES + " ("
                            + getMaxBacklogMinutes() + " minutes) must stay well below "
                            + DELETE_EVENTS_OLDER_THAN + " (" + getRetentionDays()
                            + " days). A node recovering from downtime may be pointed at events the "
                            + "purge job has already deleted, and will believe it has caught up.");
            return false;
        }
        return true;
    }

    private static int positiveOrDefault(final int value, final int defaultValue, final String key) {
        if (value > 0) {
            return value;
        }
        Logger.warn(SystemEventsConfig.class, "Ignoring non-positive value [" + value + "] for ["
                + key + "]; falling back to the default [" + defaultValue + "].");
        return defaultValue;
    }
}
