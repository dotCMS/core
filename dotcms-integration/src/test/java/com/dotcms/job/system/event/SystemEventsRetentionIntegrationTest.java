package com.dotcms.job.system.event;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the interaction between the delivery cursor and {@code DeleteOldSystemEventsJob} (issue
 * #36827).
 *
 * <p>The hazard being guarded against: the purge job removes rows older than
 * {@code DELETE_EVENTS_OLDER_THAN}, while a node returning from downtime reaches back as far as
 * {@code SYSTEM_EVENTS_MAX_BACKLOG_MINUTES}. If the clamp were allowed to point past the purge
 * horizon, the node would read an empty range and conclude it had caught up — silently, which is the
 * precise failure mode this whole fix exists to eliminate.
 */
public class SystemEventsRetentionIntegrationTest {

    private static SystemEventsAPI systemEventsAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemEventsAPI = SystemEventsFactory.getInstance().getSystemEventsAPI();
    }

    /**
     * Method to test: {@link SystemEventsConfig#isBacklogWithinRetention()} against the real
     * retention setting
     * Given Scenario: The shipped defaults
     * ExpectedResult: The backlog clamp sits well inside the retention window, so a recovering node
     * can never be pointed at rows the purge job has already deleted
     */
    @Test
    public void test_default_backlog_clamp_stays_well_inside_retention() {
        assertTrue("The shipped defaults must be self-consistent",
                SystemEventsConfig.isBacklogWithinRetention());

        final long backlogMillis = SystemEventsConfig.getMaxBacklogMillis();
        final long retentionMillis = TimeUnit.DAYS.toMillis(SystemEventsConfig.getRetentionDays());

        assertTrue("The clamp must leave a real margin, not merely fit inside retention",
                backlogMillis * 2 <= retentionMillis);
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#beginPoll(Long, long)} against the purge
     * horizon
     * Given Scenario: A node whose persisted cursor predates the retention window entirely — it was
     * down far longer than events are kept
     * ExpectedResult: The cursor is clamped, the clamp is reported with the span it skipped, and the
     * resulting read floor lies inside the retention window rather than in purged territory
     */
    @Test
    public void test_cursor_older_than_retention_is_clamped_inside_the_retention_window() {
        final long now = System.currentTimeMillis();
        final long retentionMillis = TimeUnit.DAYS.toMillis(SystemEventsConfig.getRetentionDays());
        final long cursorFromBeforeRetention = now - retentionMillis - TimeUnit.DAYS.toMillis(5);

        final SystemEventsCursorTracker tracker = new SystemEventsCursorTracker(
                SystemEventsConfig.getOverlapWindowMillis(), SystemEventsConfig.getMaxBacklogMillis());
        final SystemEventsPollWindow window = tracker.beginPoll(cursorFromBeforeRetention, now);

        assertTrue("A cursor older than retention must be clamped", window.isClamped());
        assertTrue("The clamp must report what it skipped rather than swallowing it",
                window.getSkippedSpanMillis() > 0);
        assertTrue("The read floor must land inside the retention window, never in purged territory",
                window.getReadFloor() > now - retentionMillis);
    }

    /**
     * Method to test: {@link SystemEventsAPI#deleteEvents(long)} versus the clamped read floor
     * Given Scenario: An event old enough for the purge job to remove
     * ExpectedResult: The purge removes it, and the clamped read floor of a badly lagging node sits
     * newer than the purge horizon — so the node is never left reading a range whose contents were
     * deleted out from under it
     */
    @Test
    public void test_purged_events_lie_outside_the_clamped_read_range() throws Exception {
        final String eventId = UUIDGenerator.generateUuid();
        final long now = System.currentTimeMillis();
        final long retentionMillis = TimeUnit.DAYS.toMillis(SystemEventsConfig.getRetentionDays());
        final long ancient = now - retentionMillis - TimeUnit.DAYS.toMillis(2);

        try {
            systemEventsAPI.push(new SystemEvent(eventId, SystemEventType.CLUSTER_WIDE_EVENT,
                    new Payload("ancient-" + eventId), new Date(ancient), null));

            assertEquals("Precondition: the ancient event exists", 1, countEvent(eventId));

            // What DeleteOldSystemEventsDelegate does on its schedule.
            systemEventsAPI.deleteEvents(now - retentionMillis);
            DbConnectionFactory.commit();

            assertEquals("The purge must remove events older than retention", 0, countEvent(eventId));

            final SystemEventsCursorTracker tracker = new SystemEventsCursorTracker(
                    SystemEventsConfig.getOverlapWindowMillis(),
                    SystemEventsConfig.getMaxBacklogMillis());
            final SystemEventsPollWindow window = tracker.beginPoll(ancient, now);

            assertTrue("The clamped floor must be newer than the purged event, so the node is not "
                    + "silently reading an emptied range", window.getReadFloor() > ancient);
        } finally {
            new DotConnect().setSQL("DELETE FROM system_event WHERE identifier = ?")
                    .addParam(eventId).loadResult();
            DbConnectionFactory.commit();
        }
    }

    private int countEvent(final String eventId) throws Exception {
        final List<Map<String, Object>> rows = new DotConnect()
                .setSQL("SELECT identifier FROM system_event WHERE identifier = ?")
                .addParam(eventId)
                .loadObjectResults();
        return rows.size();
    }
}
