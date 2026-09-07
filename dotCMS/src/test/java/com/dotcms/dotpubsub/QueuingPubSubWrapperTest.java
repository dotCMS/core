package com.dotcms.dotpubsub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Covers the failure accounting added for issue #36803.
 *
 * {@link QueuingPubSubWrapper#publish} returns {@code true} before the wrapped provider has
 * actually published, so the caller can never see a failure. Previously the result of the
 * submitted task was discarded outright, which is what let a transport that was initialized but
 * failing every publish look completely healthy.
 */
public class QueuingPubSubWrapperTest {

    private static final String CACHE_TOPIC = "dotcache_topic";
    private static final String OTHER_TOPIC = "osgi_topic";

    private static final long AWAIT_MILLIS = 5000;

    /**
     * Waits for the wrapper's submitter thread to drain, since publish() is asynchronous.
     */
    private static void awaitFailures(final QueuingPubSubWrapper wrapper, final String topic,
                                     final long expected) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + AWAIT_MILLIS;
        while (System.currentTimeMillis() < deadline
                && wrapper.getFailedPublishCount(topic) < expected) {
            Thread.sleep(25);
        }
    }

    private static DotPubSubEvent event(final String topic, final String message) {
        return new DotPubSubEvent.Builder().withTopic(topic).withMessage(message).build();
    }

    /**
     * Method to test: {@link QueuingPubSubWrapper#getFailedPublishCount(String)}
     * Given Scenario: the wrapped provider returns false for every publish, as a synchronous
     * provider with a dropped connection does.
     * Expected Result: each failure is counted even though publish() reported success to the
     * caller.
     */
    @Test
    public void test_failed_publishes_are_counted() throws Exception {
        final QueuingPubSubWrapper wrapper = new QueuingPubSubWrapper(new AlwaysFailsProvider());

        assertTrue("publish() reports success regardless - that is why the count is needed",
                wrapper.publish(event(CACHE_TOPIC, "inval-1")));
        assertTrue(wrapper.publish(event(CACHE_TOPIC, "inval-2")));

        awaitFailures(wrapper, CACHE_TOPIC, 2);
        assertEquals(2, wrapper.getFailedPublishCount(CACHE_TOPIC));
    }

    /**
     * Method to test: {@link QueuingPubSubWrapper#getFailedPublishCount(String)}
     * Given Scenario: a provider that throws instead of returning false.
     * Expected Result: the throw is counted rather than lost on the submitter thread.
     */
    @Test
    public void test_thrown_publish_failures_are_counted() throws Exception {
        final QueuingPubSubWrapper wrapper = new QueuingPubSubWrapper(new ThrowingProvider());

        wrapper.publish(event(CACHE_TOPIC, "inval-throw"));

        awaitFailures(wrapper, CACHE_TOPIC, 1);
        assertEquals(1, wrapper.getFailedPublishCount(CACHE_TOPIC));
    }

    /**
     * Method to test: {@link QueuingPubSubWrapper#getFailedPublishCount(String)}
     * Given Scenario: one provider instance is shared by every topic in the JVM, and a non-cache
     * topic fails.
     * Expected Result: the failure is attributed to its own topic only, so the cache transport's
     * metric is not inflated by OSGi or cluster-management traffic.
     */
    @Test
    public void test_failures_are_attributed_per_topic() throws Exception {
        final QueuingPubSubWrapper wrapper = new QueuingPubSubWrapper(new AlwaysFailsProvider());

        wrapper.publish(event(OTHER_TOPIC, "osgi-restart"));

        awaitFailures(wrapper, OTHER_TOPIC, 1);
        assertEquals(1, wrapper.getFailedPublishCount(OTHER_TOPIC));
        assertEquals("cache topic saw no traffic and must report no failures",
                0, wrapper.getFailedPublishCount(CACHE_TOPIC));
    }

    /**
     * Method to test: {@link QueuingPubSubWrapper#getFailedPublishCount(String)}
     * Given Scenario: the wrapped provider publishes successfully.
     * Expected Result: nothing is counted, and a topic that was never published to reports 0
     * rather than failing on a missing counter.
     */
    @Test
    public void test_successful_publishes_are_not_counted() throws Exception {
        final CountingProvider provider = new CountingProvider();
        final QueuingPubSubWrapper wrapper = new QueuingPubSubWrapper(provider);

        wrapper.publish(event(CACHE_TOPIC, "inval-ok"));

        final long deadline = System.currentTimeMillis() + AWAIT_MILLIS;
        while (System.currentTimeMillis() < deadline && provider.published.get() == 0) {
            Thread.sleep(25);
        }

        assertEquals(1, provider.published.get());
        assertEquals(0, wrapper.getFailedPublishCount(CACHE_TOPIC));
        assertEquals(0, wrapper.getFailedPublishCount("never-used-topic"));
    }

    /**
     * Method to test: {@link QueuingPubSubWrapper#getFailedPublishCount(String)}
     * Given Scenario: the failure is recorded from an event, whose topic
     * {@code DotPubSubEvent.Builder.withTopic} has lowercased, but the lookup arrives with the
     * topic key un-normalized -- which is what {@code DotPubSubTopic.getTopic()} returns, since it
     * is a bare {@code String.valueOf(getKey())}.
     * Expected Result: the count is found regardless of case. Without normalization this misses
     * silently and reports zero failures while invalidations are being lost, which is the same
     * class of blind spot #36803 removes. It happens to work today only because every current
     * topic key is already lowercase.
     */
    @Test
    public void test_failure_lookup_is_case_insensitive() throws Exception {
        final QueuingPubSubWrapper wrapper = new QueuingPubSubWrapper(new AlwaysFailsProvider());

        wrapper.publish(event("DotCache_Topic", "inval-mixed-case"));

        awaitFailures(wrapper, "dotcache_topic", 1);
        assertEquals("recorded under the lowercased key the event builder produced",
                1, wrapper.getFailedPublishCount("dotcache_topic"));
        assertEquals("and still found when the caller passes the un-normalized topic key",
                1, wrapper.getFailedPublishCount("DotCache_Topic"));
        assertEquals("any casing resolves to the same counter",
                1, wrapper.getFailedPublishCount("DOTCACHE_TOPIC"));
    }

    private static class AlwaysFailsProvider extends NullDotPubSubProvider {
        @Override
        public boolean publish(final DotPubSubEvent event) {
            return false;
        }
    }

    private static class ThrowingProvider extends NullDotPubSubProvider {
        @Override
        public boolean publish(final DotPubSubEvent event) {
            throw new IllegalStateException("pubsub connection is gone");
        }
    }

    private static class CountingProvider extends NullDotPubSubProvider {
        final AtomicInteger published = new AtomicInteger(0);

        @Override
        public boolean publish(final DotPubSubEvent event) {
            published.incrementAndGet();
            return true;
        }
    }
}
