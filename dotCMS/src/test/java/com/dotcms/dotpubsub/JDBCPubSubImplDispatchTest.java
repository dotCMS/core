package com.dotcms.dotpubsub;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link JDBCPubSubImpl#dispatch(DotPubSubEvent, String, java.util.Collection)}: one
 * topic handler that throws must not stop the rest of a Postgres notification batch from being
 * delivered, cache invalidations included (issue #37876).
 *
 * @author hassandotcms
 */
public class JDBCPubSubImplDispatchTest {

    private static final String CHANNEL = "dotcache_topic";

    /** A topic on {@link #CHANNEL} that counts what it receives and can be told to throw from its handler. */
    private static class FakeTopic implements DotPubSubTopic {

        private final boolean failOnNotify;
        int notified;
        int counted;

        FakeTopic(final boolean failOnNotify) {
            this.failOnNotify = failOnNotify;
        }

        @Override
        public Comparable getKey() {
            return CHANNEL;
        }

        @Override
        public void incrementReceivedCounters(final DotPubSubEvent event) {
            counted++;
        }

        @Override
        public void notify(final DotPubSubEvent event) {
            if (failOnNotify) {
                throw new IllegalStateException("handler failure");
            }
            notified++;
        }
    }

    private static DotPubSubEvent event() {
        return new DotPubSubEvent.Builder().withTopic(CHANNEL).withType("INVAL").withMessage("0:group").build();
    }

    /**
     * Given : three topics on the same channel, the middle one throwing from notify().
     * When  : an event is dispatched to them.
     * Then  : the first and third topics still get the event and count it, and nothing escapes.
     */
    @Test
    public void handlerFailure_doesNotStopOtherTopics() {
        final FakeTopic first = new FakeTopic(false);
        final FakeTopic failing = new FakeTopic(true);
        final FakeTopic third = new FakeTopic(false);

        JDBCPubSubImpl.dispatch(event(), CHANNEL, List.of(first, failing, third));

        assertEquals(1, first.notified);
        assertEquals(1, first.counted);
        assertEquals(1, third.notified);
        assertEquals(1, third.counted);
    }
}
