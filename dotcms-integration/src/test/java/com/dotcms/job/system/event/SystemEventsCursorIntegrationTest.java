package com.dotcms.job.system.event;

import com.dotcms.api.system.event.SystemEventsCursor;
import com.dotcms.api.system.event.SystemEventsCursorAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the durable per-node delivery cursor (issue #36827).
 *
 * <p>Two properties are pinned here that a unit test cannot reach:
 *
 * <ol>
 *   <li><b>The cursor survives a restart.</b> The original code kept the mark in a
 *       {@code static volatile AtomicLong}, so every restart reset it to "now" and every event
 *       committed while the node was down was skipped permanently. That loss vector is independent
 *       of the commit-timing race the issue reports, and it is fixed by persistence alone.</li>
 *   <li><b>Single-node installations stay correct and cheap</b> — exactly one cursor row per node,
 *       written by upsert, so the new state cannot grow without bound (AC-011). Routing behaviour
 *       (a node skipping events it authored) is covered by the delegate tests in US3, where the
 *       delegate is actually in the loop.</li>
 * </ol>
 */
public class SystemEventsCursorIntegrationTest {

    private static SystemEventsCursorAPI cursorAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        cursorAPI = SystemEventsFactory.getInstance().getSystemEventsCursorAPI();
    }

    /**
     * Method to test: {@link SystemEventsCursorAPI#save(String, long)} and
     * {@link SystemEventsCursorAPI#findByServerId(String)}
     * Given Scenario: A cursor is stored for a node, then read back through a freshly obtained API
     * reference — standing in for the node restarting with no in-memory state
     * ExpectedResult: The cursor value survives; it is read from the database, not from a field
     */
    @Test
    public void test_cursor_survives_a_restart() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();
        final long cursorValue = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);

        try {
            cursorAPI.save(serverId, cursorValue);

            // A new API reference stands in for a restarted JVM holding no in-memory mark.
            final SystemEventsCursorAPI afterRestart =
                    SystemEventsFactory.getInstance().getSystemEventsCursorAPI();
            final Optional<SystemEventsCursor> recovered = afterRestart.findByServerId(serverId);

            assertTrue("The cursor must be recoverable after a restart", recovered.isPresent());
            assertEquals(cursorValue, recovered.get().getLastEventDate());
        } finally {
            deleteCursor(serverId);
        }
    }

    /**
     * Method to test: {@link SystemEventsCursorAPI#findByServerId(String)}
     * Given Scenario: A node that has never polled before
     * ExpectedResult: Empty — an absent row is a meaningful state (seed at now), not an error and not
     * a zero that would replay the whole retention window
     */
    @Test
    public void test_absent_cursor_is_reported_as_empty() throws Exception {
        assertFalse(cursorAPI.findByServerId(UUIDGenerator.generateUuid()).isPresent());
    }

    /**
     * Method to test: {@link SystemEventsCursorAPI#save(String, long)}
     * Given Scenario: The same node saves its cursor repeatedly, as it does on every 5-second poll
     * ExpectedResult: Exactly one row per node — save is an upsert. On a single-node installation
     * this is the whole of the new state, and it must not grow (AC-011).
     */
    @Test
    public void test_repeated_saves_keep_exactly_one_row_per_node() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();

        try {
            final long base = System.currentTimeMillis();
            for (int i = 0; i < 5; i++) {
                cursorAPI.save(serverId, base + i);
            }

            final List<Map<String, Object>> rows = new DotConnect()
                    .setSQL("SELECT server_id FROM system_event_cursor WHERE server_id = ?")
                    .addParam(serverId)
                    .loadObjectResults();

            assertEquals("save() must upsert, never accumulate rows", 1, rows.size());
            assertEquals(base + 4, cursorAPI.findByServerId(serverId).get().getLastEventDate());
        } finally {
            deleteCursor(serverId);
        }
    }

    /**
     * Method to test: the cursor's interaction with events committed during downtime
     * Given Scenario: An event row exists whose created timestamp falls after the node's persisted
     * cursor — i.e. it was written while that node was down
     * ExpectedResult: A read from the persisted cursor still returns the event. Under the original
     * in-memory mark this event was unreachable, because the restart reset the mark to "now".
     */
    @Test
    public void test_event_committed_during_downtime_is_still_reachable() throws Exception {
        final String serverId = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();
        final long cursorBeforeDowntime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        final String eventId = UUIDGenerator.generateUuid();

        try {
            cursorAPI.save(serverId, cursorBeforeDowntime);

            // Written by another node while this one was down.
            insertEvent(eventId, foreignServerId,
                    System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));

            final long persisted = cursorAPI.findByServerId(serverId).get().getLastEventDate();
            final List<Map<String, Object>> visible = new DotConnect()
                    .setSQL("SELECT identifier FROM system_event WHERE created >= ? AND identifier = ?")
                    .addParam(persisted)
                    .addParam(eventId)
                    .loadObjectResults();

            assertEquals("An event committed during downtime must still be reachable from the "
                    + "persisted cursor", 1, visible.size());
        } finally {
            new DotConnect().setSQL("DELETE FROM system_event WHERE identifier = ?")
                    .addParam(eventId).loadResult();
            DbConnectionFactory.commit();
            deleteCursor(serverId);
        }
    }

    private void insertEvent(final String eventId, final String serverId, final long created)
            throws Exception {
        new DotConnect()
                .setSQL("INSERT INTO system_event (identifier, event_type, payload, created, server_id) "
                        + "VALUES (?, ?, ?, ?, ?)")
                .addParam(eventId)
                .addParam("CLUSTER_WIDE_EVENT")
                .addParam("{}")
                .addParam(created)
                .addParam(serverId)
                .loadResult();
        DbConnectionFactory.commit();
    }

    private void deleteCursor(final String serverId) throws Exception {
        new DotConnect().setSQL("DELETE FROM system_event_cursor WHERE server_id = ?")
                .addParam(serverId).loadResult();
        DbConnectionFactory.commit();
    }
}
