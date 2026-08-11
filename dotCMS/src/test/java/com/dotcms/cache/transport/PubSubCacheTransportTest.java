package com.dotcms.cache.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.dotpubsub.NullDotPubSubProvider;
import org.junit.Test;

/**
 * Unit tests for the silent-invalidation-drop fixes from issue #36803.
 */
public class PubSubCacheTransportTest {

    private PubSubCacheTransport newTransport(final NullDotPubSubProvider provider) {
        return new PubSubCacheTransport(provider, new CacheTransportTopic("fakeServer", provider));
    }

    /**
     * send() before init() must not publish, but must count the dropped message
     * instead of discarding it silently.
     */
    @Test
    public void test_send_before_init_counts_dropped_messages() throws Exception {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final PubSubCacheTransport transport = newTransport(provider);

        assertEquals(0, transport.getDroppedMessages());

        transport.send("0:testGroup");
        transport.send("0:testGroup");

        assertEquals(2, transport.getDroppedMessages());
        assertNull("nothing may be published while uninitialized", provider.lastEventOut());
        assertFalse(transport.isInitialized());
    }

    /**
     * After init(), send() publishes and the dropped counter stops growing.
     */
    @Test
    public void test_send_after_init_publishes() throws Exception {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final PubSubCacheTransport transport = newTransport(provider);

        transport.send("dropped-before-init");
        transport.init(null);
        assertTrue(transport.isInitialized());

        transport.send("0:testGroup");

        assertNotNull(provider.lastEventOut());
        assertEquals(1, transport.getDroppedMessages());
    }

    /**
     * init() is idempotent: calling it on an already-initialized transport is a no-op
     * rather than a second pubsub start/subscribe cycle (the rewire loop from #36544).
     */
    @Test
    public void test_init_is_idempotent() throws Exception {
        final CountingProvider provider = new CountingProvider();
        final PubSubCacheTransport transport =
                new PubSubCacheTransport(provider, new CacheTransportTopic("fakeServer", provider));

        transport.init(null);
        transport.init(null);
        transport.init(null);

        assertEquals(1, provider.starts);
        assertTrue(transport.isInitialized());

        // after shutdown, init() must run again
        transport.shutdown();
        assertFalse(transport.isInitialized());
        transport.init(null);
        assertEquals(2, provider.starts);
    }

    /**
     * The scenario the health check could not see before: init() succeeded, so the transport
     * reports initialized and counts no drops, but every publish is failing. Providers signal
     * that by returning false rather than throwing, and send() used to discard the boolean.
     */
    @Test
    public void test_send_after_init_counts_failed_publishes() throws Exception {
        final FailingProvider provider = new FailingProvider();
        final PubSubCacheTransport transport =
                new PubSubCacheTransport(provider, new CacheTransportTopic("fakeServer", provider));

        transport.init(null);
        assertTrue(transport.isInitialized());

        transport.send("0:testGroup");
        transport.send("0:testGroup");

        assertEquals("a failing publish is not a drop - it was attempted",
                0, transport.getDroppedMessages());
        assertEquals("failed publishes must be counted, not discarded",
                2, transport.getFailedMessages());
        assertTrue("the transport still believes it is initialized", transport.isInitialized());
    }

    /**
     * A provider that publishes successfully must not inflate the failure counter.
     */
    @Test
    public void test_send_after_init_counts_no_failures_when_publish_succeeds() throws Exception {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final PubSubCacheTransport transport = newTransport(provider);

        transport.init(null);
        transport.send("0:testGroup");

        assertEquals(0, transport.getFailedMessages());
        assertEquals(0, transport.getDroppedMessages());
    }

    /**
     * An asynchronous provider returns true from publish() before the send has happened, so it
     * reports its own failures per topic instead. getFailedMessages() must pick those up, and
     * must not double count them against send()'s own synchronous tally.
     */
    @Test
    public void test_getFailedMessages_includes_async_provider_failures() throws Exception {
        final AsyncFailingProvider provider = new AsyncFailingProvider();
        final PubSubCacheTransport transport =
                new PubSubCacheTransport(provider, new CacheTransportTopic("fakeServer", provider));

        transport.init(null);
        transport.send("0:testGroup");
        transport.send("0:testGroup");

        assertEquals("send() saw only the optimistic true, so it counted nothing itself",
                0, transport.failedMessages.get());
        assertEquals("provider-reported failures must still surface", 2,
                transport.getFailedMessages());
    }

    private static class CountingProvider extends NullDotPubSubProvider {
        int starts = 0;

        @Override
        public com.dotcms.dotpubsub.DotPubSubProvider start() {
            starts++;
            return this;
        }
    }

    /**
     * Mimics a synchronous provider whose connection has dropped: publish() returns false and
     * never throws, exactly as JDBCPubSubImpl/PostgresPubSubImpl/RedisPubSubImpl do.
     */
    private static class FailingProvider extends NullDotPubSubProvider {
        @Override
        public boolean publish(final com.dotcms.dotpubsub.DotPubSubEvent event) {
            return false;
        }
    }

    /**
     * Mimics QueuingPubSubWrapper: publish() returns true immediately and the real outcome is
     * reported later through getFailedPublishCount().
     */
    private static class AsyncFailingProvider extends NullDotPubSubProvider {
        private long failures = 0;

        @Override
        public boolean publish(final com.dotcms.dotpubsub.DotPubSubEvent event) {
            failures++;
            return true;
        }

        @Override
        public long getFailedPublishCount(final String topic) {
            return failures;
        }
    }
}
