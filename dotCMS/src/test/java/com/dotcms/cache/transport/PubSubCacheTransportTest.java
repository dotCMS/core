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

    private static class CountingProvider extends NullDotPubSubProvider {
        int starts = 0;

        @Override
        public com.dotcms.dotpubsub.DotPubSubProvider start() {
            starts++;
            return this;
        }
    }
}
