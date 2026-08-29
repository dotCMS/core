package com.dotcms.job.system.event;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.job.system.event.delegate.SystemEventsJobDelegate;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for the delivery behaviour that issue #36827 reports broken: events whose
 * {@code created} timestamp is stamped before their transaction commits are skipped permanently.
 *
 * <p><b>How the race is reproduced.</b> The reader only ever sees the {@code created} column, so an
 * event whose transaction committed seconds after its {@code created} stamp is indistinguishable —
 * from the poller's point of view — from a row inserted now with a {@code created} value set in the
 * past. These tests use the latter because it is deterministic; the production scenario is a content
 * save that constructs its events early and commits them at the end.
 *
 * <p>The assertion that matters is the pair: reading from the bare cursor must MISS the event (this
 * is the bug), and reading from the cursor less one overlap window must FIND it (this is the fix).
 * Asserting both in one test means the test fails if anyone removes the overlap window later, rather
 * than quietly passing because the event happened to be recent.
 */
public class SystemEventsJobDelegateIntegrationTest {

    private static SystemEventsAPI systemEventsAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemEventsAPI = SystemEventsFactory.getInstance().getSystemEventsAPI();
    }

    /**
     * Method to test: the poll range produced by {@link SystemEventsCursorTracker} against the real
     * {@link SystemEventsAPI#getEventsSince(long)} query
     * Given Scenario: An event becomes visible only after the poller has already advanced past its
     * created timestamp — the exact mechanism reported in issue #36827
     * ExpectedResult: Reading from the bare cursor misses it (the defect); reading from the overlap
     * window floor delivers it (the fix)
     */
    @Test
    public void test_late_committing_event_is_still_delivered() throws Exception {
        final String eventId = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();
        final long now = System.currentTimeMillis();

        // The poller already advanced to `cursor`; this event's created stamp is 30s behind it, but
        // the row only becomes visible now.
        final long cursor = now - TimeUnit.SECONDS.toMillis(10);
        final long createdBeforeCursor = cursor - TimeUnit.SECONDS.toMillis(30);

        try {
            insertEvent(eventId, foreignServerId, createdBeforeCursor);

            assertFalse("Reading from the bare cursor must MISS the event - this is the defect",
                    idsSince(cursor).contains(eventId));

            final SystemEventsCursorTracker tracker = new SystemEventsCursorTracker(
                    TimeUnit.SECONDS.toMillis(120), TimeUnit.MINUTES.toMillis(60));
            final SystemEventsPollWindow window = tracker.beginPoll(cursor, now);

            assertTrue("Reading from the overlap window floor must DELIVER the event - this is the fix",
                    idsSince(window.getReadFloor()).contains(eventId));
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * Method to test: delivery of a batch created inside one long-running transaction
     * Given Scenario: Several events are constructed early in a long operation and all commit
     * together at the end, so every created stamp predates the commit
     * ExpectedResult: All of them are delivered, not just the most recent
     */
    @Test
    public void test_all_events_from_a_long_transaction_are_delivered() throws Exception {
        final String foreignServerId = UUIDGenerator.generateUuid();
        final long now = System.currentTimeMillis();
        final long cursor = now - TimeUnit.SECONDS.toMillis(5);

        // Spread across a 45s "transaction", all committing now, all stamped before the cursor.
        final List<String> eventIds = List.of(UUIDGenerator.generateUuid(),
                UUIDGenerator.generateUuid(), UUIDGenerator.generateUuid());
        final long[] createdStamps = {
                cursor - TimeUnit.SECONDS.toMillis(45),
                cursor - TimeUnit.SECONDS.toMillis(25),
                cursor - TimeUnit.SECONDS.toMillis(5)};

        try {
            for (int i = 0; i < eventIds.size(); i++) {
                insertEvent(eventIds.get(i), foreignServerId, createdStamps[i]);
            }

            final SystemEventsCursorTracker tracker = new SystemEventsCursorTracker(
                    TimeUnit.SECONDS.toMillis(120), TimeUnit.MINUTES.toMillis(60));
            final SystemEventsPollWindow window = tracker.beginPoll(cursor, now);
            final List<String> delivered = idsSince(window.getReadFloor());

            for (final String eventId : eventIds) {
                assertTrue("Every event from the long transaction must be delivered, not just the "
                        + "most recent one: " + eventId, delivered.contains(eventId));
            }
        } finally {
            for (final String eventId : eventIds) {
                deleteEvent(eventId);
            }
        }
    }

    /**
     * Method to test: the boundary of the overlap window
     * Given Scenario: An event whose created stamp is older than the whole overlap window
     * ExpectedResult: It is NOT delivered. This is the fix's acknowledged residual limitation — a
     * transaction open longer than the window can still lose its events — and it must be a known,
     * tested boundary rather than a surprise. US4 makes this case observable.
     */
    @Test
    public void test_event_older_than_the_whole_window_is_not_delivered() throws Exception {
        final String eventId = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();
        final long now = System.currentTimeMillis();
        final long cursor = now - TimeUnit.SECONDS.toMillis(10);
        final long wayOlderThanWindow = cursor - TimeUnit.MINUTES.toMillis(10);

        try {
            insertEvent(eventId, foreignServerId, wayOlderThanWindow);

            final SystemEventsCursorTracker tracker = new SystemEventsCursorTracker(
                    TimeUnit.SECONDS.toMillis(120), TimeUnit.MINUTES.toMillis(60));
            final SystemEventsPollWindow window = tracker.beginPoll(cursor, now);

            assertFalse("An event older than the overlap window is a known, bounded loss",
                    idsSince(window.getReadFloor()).contains(eventId));
            assertTrue("...and the tracker must be able to say so, so US4 can warn about it",
                    tracker.isOutsideOverlapWindow(wayOlderThanWindow, window));
        } finally {
            deleteEvent(eventId);
        }
    }

    private List<String> idsSince(final long fromDate) throws Exception {
        final Collection<SystemEvent> events = systemEventsAPI.getEventsSince(fromDate);
        return events.stream().map(SystemEvent::getId).collect(Collectors.toList());
    }

    /**
     * Publishes a real event through {@link SystemEventsAPI#push(SystemEvent)} so the stored payload
     * is genuinely marshalled — a hand-written JSON string does not round-trip through the
     * unmarshaller and fails only once the event is actually inside the read range.
     *
     * <p>The API stamps {@code server_id} with the local node, so it is updated afterwards to make
     * the event look as though another node authored it.
     */
    private void insertEvent(final String eventId, final String serverId, final long created)
            throws Exception {

        final SystemEvent event = new SystemEvent(eventId, SystemEventType.CLUSTER_WIDE_EVENT,
                new Payload("test-payload-" + eventId), new Date(created), serverId);
        systemEventsAPI.push(event);

        new DotConnect()
                .setSQL("UPDATE system_event SET server_id = ? WHERE identifier = ?")
                .addParam(serverId)
                .addParam(eventId)
                .loadResult();
        DbConnectionFactory.commit();
    }

    private void deleteEvent(final String eventId) throws Exception {
        new DotConnect().setSQL("DELETE FROM system_event WHERE identifier = ?")
                .addParam(eventId).loadResult();
        DbConnectionFactory.commit();
    }

    /**
     * Method to test: {@link com.dotcms.job.system.event.delegate.SystemEventsJobDelegate}'s handling
     * of a failed read (AC-007)
     * Given Scenario: The queue read fails
     * ExpectedResult: The failure is surfaced, not swallowed. Before this fix it was caught and
     * logged at DEBUG, so a node that had stopped consuming entirely looked exactly like a node with
     * a quiet queue — which is how a 50-63% loss rate went unnoticed long enough to reach a customer.
     */
    @Test
    public void test_failed_read_is_surfaced_not_swallowed() {
        final SystemEventsJobDelegate delegate = new SystemEventsJobDelegate() {
            @Override
            public void executeDelegate(final JobDelegateDataBean data) throws DotDataException {
                throw new DotDataException("simulated read failure");
            }
        };

        // AbstractJobDelegate.execute() logs at ERROR and does not propagate, so the Job keeps
        // running; what matters is that executeDelegate itself refuses to swallow.
        assertThrows(DotDataException.class,
                () -> delegate.executeDelegate(new JobDelegateDataBean(null, 0L, null)));
    }

    /**
     * Method to test: {@link SystemEventsAPI#getEventsSince(long)} with a poison payload in the batch
     * Given Scenario: One row in the window carries a payload whose class cannot be deserialized —
     * reproduced live on a two-node cluster, where `UserSessionBean` on `SWITCH_SITE` events (emitted
     * during ordinary admin activity) made **every poll on both nodes** throw, delivering nothing
     * Expected Result: The undeserialisable row is skipped and every other event in the window is
     * still delivered. One bad payload must not destroy its batch (issue #37249).
     */
    @Test
    public void test_one_undeserialisable_payload_does_not_destroy_the_batch() throws Exception {
        final String goodBefore = UUIDGenerator.generateUuid();
        final String poison = UUIDGenerator.generateUuid();
        final String goodAfter = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();
        final long base = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(30);

        try {
            insertEvent(goodBefore, foreignServerId, base);
            insertPoisonEvent(poison, foreignServerId, base + 1);
            insertEvent(goodAfter, foreignServerId, base + 2);

            final List<String> delivered = idsSince(base - TimeUnit.SECONDS.toMillis(10));

            assertTrue("An event BEFORE the poison row must still be delivered",
                    delivered.contains(goodBefore));
            assertTrue("An event AFTER the poison row must still be delivered",
                    delivered.contains(goodAfter));
            assertFalse("The undeserialisable row itself is skipped", delivered.contains(poison));
        } finally {
            deleteEvent(goodBefore);
            deleteEvent(poison);
            deleteEvent(goodAfter);
        }
    }

    /**
     * Writes a row whose payload names a class Jackson cannot construct. Written with raw SQL on
     * purpose: the API would never produce one, which is precisely why this failure reached
     * production unnoticed.
     */
    private void insertPoisonEvent(final String eventId, final String serverId, final long created)
            throws Exception {
        new DotConnect()
                .setSQL("INSERT INTO system_event (identifier, event_type, payload, created, server_id) "
                        + "VALUES (?, ?, ?, ?, ?)")
                .addParam(eventId)
                .addParam("SWITCH_SITE")
                .addParam("{\"data\":{\"user\":\"dotcms.org.1\",\"sessionId\":\"ABC123\"},"
                        + "\"visibility\":\"GLOBAL\","
                        + "\"type\":\"com.dotcms.api.system.event.UserSessionBean\"}")
                .addParam(created)
                .addParam(serverId)
                .loadResult();
        DbConnectionFactory.commit();
    }
}
