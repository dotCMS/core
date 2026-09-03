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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the in-product authored-vs-observed reconciliation (issue #36827, AC-007 / AC-008).
 *
 * <p>AC-008 sets a ≤1% loss tolerance measured by hand over 24 hours on a two-node cluster. That is a
 * one-off exercise involving SQL and log-line counting. This makes the same measurement continuous
 * and in-product, so a regression shows up on its own instead of waiting for someone to go looking
 * after the next support case.
 *
 * <p>The comparison is deliberately of a node against <b>itself</b>: every node observes every event
 * it authored (and then skips it), so authored-by-me and observed-by-me must match. That is the same
 * property the issue used to measure the original 50.5% and 63.4% loss.
 */
public class SystemEventsReconciliationIntegrationTest {

    private static SystemEventsAPI systemEventsAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemEventsAPI = SystemEventsFactory.getInstance().getSystemEventsAPI();
    }

    /**
     * Method to test: {@link SystemEventsReconciliation#reconcile(String, long, long)}
     * Given Scenario: Every event a node authored in the window was also observed by its poller
     * ExpectedResult: Zero loss, and the result reports itself as within tolerance
     */
    @Test
    public void test_full_delivery_reports_zero_loss() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();
        final List<String> ids = pushEvents(serverId, 10, System.currentTimeMillis());

        try {
            final SystemEventsReconciliation reconciliation = new SystemEventsReconciliation();
            final SystemEventsReconciliation.Result result =
                    reconciliation.reconcile(serverId, TimeUnit.MINUTES.toMillis(10), 10L);

            assertEquals(10L, result.getAuthoredCount());
            assertEquals(10L, result.getObservedCount());
            assertEquals(0.0d, result.getLossPercent(), 0.001d);
            assertTrue("Zero loss must be within tolerance", result.isWithinTolerance());
        } finally {
            deleteEvents(ids);
        }
    }

    /**
     * Method to test: {@link SystemEventsReconciliation#reconcile(String, long, long)}
     * Given Scenario: A node authored 10 events in the window but its poller observed only 4 — the
     * shape of the originally reported defect, at roughly the reported magnitude
     * ExpectedResult: 60% loss reported, and flagged as outside the ≤1% tolerance
     */
    @Test
    public void test_partial_delivery_is_flagged_outside_tolerance() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();
        final List<String> ids = pushEvents(serverId, 10, System.currentTimeMillis());

        try {
            final SystemEventsReconciliation.Result result = new SystemEventsReconciliation()
                    .reconcile(serverId, TimeUnit.MINUTES.toMillis(10), 4L);

            assertEquals(10L, result.getAuthoredCount());
            assertEquals(4L, result.getObservedCount());
            assertEquals(60.0d, result.getLossPercent(), 0.001d);
            assertFalse("60% loss is the reported defect; it must not pass tolerance",
                    result.isWithinTolerance());
        } finally {
            deleteEvents(ids);
        }
    }

    /**
     * Method to test: the ≤1% tolerance boundary (AC-008)
     * Given Scenario: 1 event missing out of 200 — 0.5%, the kind of gap a restart or the purge
     * boundary can produce mid-measurement
     * ExpectedResult: Within tolerance. The bar exists to absorb exactly this, without excusing real
     * loss.
     */
    @Test
    public void test_small_gap_stays_within_the_agreed_tolerance() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();
        final List<String> ids = pushEvents(serverId, 200, System.currentTimeMillis());

        try {
            final SystemEventsReconciliation.Result result = new SystemEventsReconciliation()
                    .reconcile(serverId, TimeUnit.MINUTES.toMillis(10), 199L);

            assertEquals(0.5d, result.getLossPercent(), 0.001d);
            assertTrue("0.5% is inside the agreed 1% bar", result.isWithinTolerance());
        } finally {
            deleteEvents(ids);
        }
    }

    /**
     * Method to test: {@link SystemEventsReconciliation#reconcile(String, long, long)}
     * Given Scenario: A node that authored nothing in the window — an idle node, or a fresh install
     * ExpectedResult: No division by zero, and reported as within tolerance rather than as 100% loss.
     * A quiet node must not raise an alarm.
     */
    @Test
    public void test_idle_node_does_not_report_loss() throws Exception {
        final SystemEventsReconciliation.Result result = new SystemEventsReconciliation()
                .reconcile(UUIDGenerator.generateUuid(), TimeUnit.MINUTES.toMillis(10), 0L);

        assertEquals(0L, result.getAuthoredCount());
        assertEquals(0.0d, result.getLossPercent(), 0.001d);
        assertTrue("An idle node must not look like a broken one", result.isWithinTolerance());
    }

    /**
     * Method to test: the window bound of the reconciliation query
     * Given Scenario: Events authored longer ago than the reconciliation window
     * ExpectedResult: They are not counted. The window has to match the period the observed count
     * covers, or the comparison is meaningless.
     */
    @Test
    public void test_events_outside_the_window_are_not_counted() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();
        final long old = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3);
        final List<String> ids = pushEvents(serverId, 5, old);

        try {
            final SystemEventsReconciliation.Result result = new SystemEventsReconciliation()
                    .reconcile(serverId, TimeUnit.MINUTES.toMillis(10), 0L);

            assertEquals("Events older than the window must not be counted as lost",
                    0L, result.getAuthoredCount());
            assertTrue(result.isWithinTolerance());
        } finally {
            deleteEvents(ids);
        }
    }

    private List<String> pushEvents(final String serverId, final int count, final long created)
            throws Exception {
        final List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String eventId = UUIDGenerator.generateUuid();
            systemEventsAPI.push(new SystemEvent(eventId, SystemEventType.CLUSTER_WIDE_EVENT,
                    new Payload("reconcile-" + eventId), new Date(created), serverId));
            ids.add(eventId);
        }
        // push() stamps the local server id; rewrite it so the events look authored by the node under
        // test.
        for (final String id : ids) {
            new DotConnect().setSQL("UPDATE system_event SET server_id = ? WHERE identifier = ?")
                    .addParam(serverId)
                    .addParam(id)
                    .loadResult();
        }
        DbConnectionFactory.commit();
        return ids;
    }

    private void deleteEvents(final List<String> ids) throws Exception {
        for (final String id : ids) {
            new DotConnect().setSQL("DELETE FROM system_event WHERE identifier = ?")
                    .addParam(id).loadResult();
        }
        DbConnectionFactory.commit();
    }
}
