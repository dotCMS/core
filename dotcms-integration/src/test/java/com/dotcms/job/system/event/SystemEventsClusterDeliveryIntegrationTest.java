package com.dotcms.job.system.event;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.SystemEvent;
import com.dotcms.api.system.event.SystemEventType;
import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.job.system.event.delegate.SystemEventsJobDelegate;
import com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Cross-node delivery tests for the system event queue (issue #36827, AC-006).
 *
 * <p>A real two-node cluster cannot run in CI, so the second node is simulated the way the delegate
 * itself distinguishes nodes: by {@code server_id}. An event row carrying a foreign {@code server_id}
 * is exactly what node B sees when node A authors an event, because both nodes read the same table
 * and the only thing telling them apart is that column.
 *
 * <p>What this cannot cover is the real network, two JVMs, and clock differences between hosts —
 * that remains the manual two-node verification in quickstart.md §4.
 */
public class SystemEventsClusterDeliveryIntegrationTest {

    private static SystemEventsAPI systemEventsAPI;
    private static String localServerId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemEventsAPI = SystemEventsFactory.getInstance().getSystemEventsAPI();
        localServerId = APILocator.getServerAPI().readServerId();
    }

    /**
     * Method to test: {@link SystemEventsJobDelegate#executeDelegate(JobDelegateDataBean)}
     * Given Scenario: Node A publishes a CLUSTER_WIDE_EVENT; node B polls the shared table
     * ExpectedResult: Node B converts it to a local notify and its subscribers are told. This is the
     * path that left a node serving stale configuration for over 24 hours in the originating support
     * case.
     */
    @Test
    public void test_cluster_wide_event_authored_elsewhere_is_delivered_locally() throws Exception {
        final String key = "DOT_TEST_KEY_" + UUIDGenerator.generateUuid();
        final String eventId = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> receivedKey = new AtomicReference<>();
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class,
                (EventSubscriber<SystemTableUpdatedKeyEvent>) event -> {
                    if (key.equals(event.getKey())) {
                        receivedKey.set(event.getKey());
                        latch.countDown();
                    }
                });

        try {
            final long created = System.currentTimeMillis();
            pushAs(eventId, foreignServerId, created, SystemEventType.CLUSTER_WIDE_EVENT,
                    new SystemTableUpdatedKeyEvent(key));

            runDelegate(created - TimeUnit.MINUTES.toMillis(1));

            assertTrue("A CLUSTER_WIDE_EVENT authored on another node must reach this node's local "
                    + "subscribers within the poll", latch.await(20, TimeUnit.SECONDS));
            assertEquals(key, receivedKey.get());
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * Method to test: {@link SystemEventsJobDelegate#executeDelegate(JobDelegateDataBean)}
     * Given Scenario: The node reads an event it authored itself
     * ExpectedResult: It is skipped, not re-delivered. The author already handled it at publish time;
     * re-notifying would double-apply every event on the node that produced it (AC-012).
     */
    @Test
    public void test_node_skips_events_it_authored_itself() throws Exception {
        final String key = "DOT_TEST_OWN_" + UUIDGenerator.generateUuid();
        final String eventId = UUIDGenerator.generateUuid();

        final CountDownLatch latch = new CountDownLatch(1);
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class,
                (EventSubscriber<SystemTableUpdatedKeyEvent>) event -> {
                    if (key.equals(event.getKey())) {
                        latch.countDown();
                    }
                });

        try {
            final long created = System.currentTimeMillis();
            // Authored by THIS node.
            pushAs(eventId, localServerId, created, SystemEventType.CLUSTER_WIDE_EVENT,
                    new SystemTableUpdatedKeyEvent(key));

            runDelegate(created - TimeUnit.MINUTES.toMillis(1));

            assertFalse("A node must not re-deliver an event it authored itself",
                    latch.await(2, TimeUnit.SECONDS));
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * Method to test: {@link SystemEventsJobDelegate#executeDelegate(JobDelegateDataBean)}
     * Given Scenario: A non-cluster event type authored on another node
     * ExpectedResult: It is NOT unwrapped onto the local bus — those go to the websocket endpoint.
     * Only CLUSTER_WIDE_EVENT is converted to a local notify (AC-012).
     */
    @Test
    public void test_non_cluster_event_is_not_notified_on_the_local_bus() throws Exception {
        final String key = "DOT_TEST_NONCLUSTER_" + UUIDGenerator.generateUuid();
        final String eventId = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();

        final CountDownLatch latch = new CountDownLatch(1);
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class,
                (EventSubscriber<SystemTableUpdatedKeyEvent>) event -> {
                    if (key.equals(event.getKey())) {
                        latch.countDown();
                    }
                });

        try {
            final long created = System.currentTimeMillis();
            pushAs(eventId, foreignServerId, created, SystemEventType.SAVE_FOLDER,
                    new SystemTableUpdatedKeyEvent(key));

            runDelegate(created - TimeUnit.MINUTES.toMillis(1));

            assertFalse("Only CLUSTER_WIDE_EVENT is unwrapped onto the local bus; other types go to "
                    + "the websocket", latch.await(2, TimeUnit.SECONDS));
        } finally {
            deleteEvent(eventId);
        }
    }

    /**
     * Method to test: dedupe across consecutive polls
     * Given Scenario: Two polls whose overlap windows both cover the same foreign event
     * ExpectedResult: It is delivered once. The overlap window is what makes late commits visible; it
     * must not also make every event inside it be delivered on every poll.
     */
    @Test
    public void test_event_inside_the_overlap_window_is_delivered_only_once() throws Exception {
        final String key = "DOT_TEST_ONCE_" + UUIDGenerator.generateUuid();
        final String eventId = UUIDGenerator.generateUuid();
        final String foreignServerId = UUIDGenerator.generateUuid();

        final CountDownLatch latch = new CountDownLatch(2);
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class,
                (EventSubscriber<SystemTableUpdatedKeyEvent>) event -> {
                    if (key.equals(event.getKey())) {
                        latch.countDown();
                    }
                });

        try {
            final long created = System.currentTimeMillis();
            pushAs(eventId, foreignServerId, created, SystemEventType.CLUSTER_WIDE_EVENT,
                    new SystemTableUpdatedKeyEvent(key));

            // One tracker across both polls, as the running Job holds.
            final SystemEventsCursorTracker tracker = new SystemEventsCursorTracker(
                    TimeUnit.SECONDS.toMillis(120), TimeUnit.MINUTES.toMillis(60));
            final long floor = created - TimeUnit.MINUTES.toMillis(1);
            runDelegate(floor, tracker);
            runDelegate(floor, tracker);

            assertFalse("The second poll must not re-deliver an event already delivered inside the "
                    + "window", latch.await(2, TimeUnit.SECONDS));
            assertEquals("Exactly one delivery expected", 1, latch.getCount());
        } finally {
            deleteEvent(eventId);
        }
    }

    private void runDelegate(final long floor) throws Exception {
        runDelegate(floor, new SystemEventsCursorTracker(TimeUnit.SECONDS.toMillis(120),
                TimeUnit.MINUTES.toMillis(60)));
    }

    private void runDelegate(final long floor, final SystemEventsCursorTracker tracker)
            throws Exception {
        new SystemEventsJobDelegate().executeDelegate(new JobDelegateDataBean(null, floor, tracker));
    }

    /**
     * Publishes a real event through the API so its payload is genuinely marshalled, then rewrites
     * {@code server_id} to make it look as though the given node authored it.
     */
    private void pushAs(final String eventId, final String serverId, final long created,
                        final SystemEventType type, final Object payloadData) throws Exception {

        systemEventsAPI.push(new SystemEvent(eventId, type, new Payload(payloadData),
                new Date(created), serverId));

        new DotConnect().setSQL("UPDATE system_event SET server_id = ? WHERE identifier = ?")
                .addParam(serverId).addParam(eventId).loadResult();
        DbConnectionFactory.commit();
    }

    private void deleteEvent(final String eventId) throws Exception {
        new DotConnect().setSQL("DELETE FROM system_event WHERE identifier = ?")
                .addParam(eventId).loadResult();
        DbConnectionFactory.commit();
    }
}
