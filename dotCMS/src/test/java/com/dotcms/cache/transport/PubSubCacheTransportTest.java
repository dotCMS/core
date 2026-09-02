package com.dotcms.cache.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.dotpubsub.NullDotPubSubProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
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
        assertEquals("the pre-init drop was retired to the startup counter",
                0, transport.getDroppedMessages());
        assertEquals(1, transport.getStartupDroppedMessages());
    }

    /**
     * A successful init() moves the drops that happened before the transport ever came up into the
     * startup counter, so getDroppedMessages() reports only invalidations lost while the transport
     * was expected to be carrying them. Without this, every node permanently reports the boot
     * burst -- measured at ~2,800 on a first boot -- and the counter is useless for alerting.
     */
    @Test
    public void test_init_retires_startup_drops_from_the_dropped_counter() throws Exception {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final PubSubCacheTransport transport = newTransport(provider);

        transport.send("boot-1");
        transport.send("boot-2");
        transport.send("boot-3");
        assertEquals(3, transport.getDroppedMessages());
        assertEquals(0, transport.getStartupDroppedMessages());

        transport.init(null);

        assertEquals("drops from before the first init are not operational drops",
                0, transport.getDroppedMessages());
        assertEquals("but they are still reported, for diagnostics",
                3, transport.getStartupDroppedMessages());
    }

    /**
     * Drops after the transport has been up once are real invalidation loss: the transport was
     * expected to be carrying them. A later re-init must not launder those into the startup
     * bucket, which would recreate the blind spot this issue exists to remove.
     */
    @Test
    public void test_drops_after_first_init_are_not_retired_by_a_later_init() throws Exception {
        final NullDotPubSubProvider provider = new NullDotPubSubProvider();
        final PubSubCacheTransport transport = newTransport(provider);

        transport.send("boot-1");
        transport.init(null);
        assertEquals(1, transport.getStartupDroppedMessages());

        // the transport goes down, and invalidations are lost while it is down
        transport.shutdown();
        assertFalse(transport.isInitialized());
        transport.send("lost-1");
        transport.send("lost-2");
        assertEquals(2, transport.getDroppedMessages());

        // recovery must leave the real loss on the books
        transport.init(null);

        assertEquals("real drops survive a re-init", 2, transport.getDroppedMessages());
        assertEquals("and are not moved into the startup bucket",
                1, transport.getStartupDroppedMessages());
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

        assertEquals(1, provider.starts());
        assertTrue(transport.isInitialized());

        // after shutdown, init() must run again
        transport.shutdown();
        assertFalse(transport.isInitialized());
        transport.init(null);
        assertEquals(2, provider.starts());
    }

    /**
     * The guard in init() must be a test-and-set, not a check-then-act. Threads racing into init()
     * all released at once must produce exactly one start()/subscribe() pair; without the
     * synchronization, several can read initialized==false and each subscribe the listener, which
     * is the double-subscribe the idempotency fix exists to prevent.
     */
    @Test
    public void test_concurrent_init_starts_and_subscribes_exactly_once() throws Exception {
        final int threads = 16;
        final CountingProvider provider = new CountingProvider();
        final PubSubCacheTransport transport =
                new PubSubCacheTransport(provider, new CacheTransportTopic("fakeServer", provider));

        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go = new CountDownLatch(1);
        final List<Throwable> failures = new ArrayList<>();
        final List<Thread> workers = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final Thread t = new Thread(() -> {
                try {
                    ready.countDown();
                    go.await();
                    transport.init(null);
                } catch (Throwable e) {
                    synchronized (failures) {
                        failures.add(e);
                    }
                }
            });
            workers.add(t);
            t.start();
        }

        ready.await();
        go.countDown();
        for (final Thread t : workers) {
            t.join();
        }

        assertTrue("no thread may fail while initializing: " + failures, failures.isEmpty());
        assertEquals("the pubsub provider must be started exactly once", 1, provider.starts());
        assertEquals("and the topic subscribed exactly once", 1, provider.subscribeCount.get());
        assertTrue(transport.isInitialized());
    }

    /**
     * A failed init() must stay retryable. This is why initialized is set only after start() and
     * subscribe() return, rather than being claimed up front by a compareAndSet guard: marking it
     * before the work would leave a thrown start() permanently flagged as initialized, so
     * isInitialized() would report a healthy transport that never subscribed and shouldReinit()
     * would stop setCluster() from ever retrying it.
     */
    @Test
    public void test_failed_init_leaves_the_transport_retryable() throws Exception {
        final BrokenStartProvider provider = new BrokenStartProvider();
        final PubSubCacheTransport transport =
                new PubSubCacheTransport(provider, new CacheTransportTopic("fakeServer", provider));

        try {
            transport.init(null);
            throw new AssertionError("init() was expected to propagate the provider failure");
        } catch (IllegalStateException expected) {
            // the provider could not reach its backend
        }

        assertFalse("a failed init must not report itself initialized", transport.isInitialized());
        assertTrue("and must ask to be re-initialized", transport.shouldReinit());

        // the next cluster rewire retries, and succeeds once the backend is reachable
        provider.failing = false;
        transport.init(null);

        assertTrue(transport.isInitialized());
        assertEquals("both the failed attempt and the successful retry reached the provider",
                2, provider.startAttempts);
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
        final AtomicInteger startCount = new AtomicInteger(0);
        final AtomicInteger subscribeCount = new AtomicInteger(0);

        int starts() {
            return startCount.get();
        }

        @Override
        public com.dotcms.dotpubsub.DotPubSubProvider start() {
            startCount.incrementAndGet();
            return this;
        }

        @Override
        public com.dotcms.dotpubsub.DotPubSubProvider subscribe(
                final com.dotcms.dotpubsub.DotPubSubTopic topic) {
            subscribeCount.incrementAndGet();
            return this;
        }
    }

    /**
     * Mimics a provider whose backing connection is unavailable, so start() throws rather than
     * returning. Flips to succeeding once {@link #failing} is cleared.
     */
    private static class BrokenStartProvider extends NullDotPubSubProvider {
        boolean failing = true;
        int startAttempts = 0;

        @Override
        public com.dotcms.dotpubsub.DotPubSubProvider start() {
            startAttempts++;
            if (failing) {
                throw new IllegalStateException("cannot reach the pubsub backend");
            }
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
