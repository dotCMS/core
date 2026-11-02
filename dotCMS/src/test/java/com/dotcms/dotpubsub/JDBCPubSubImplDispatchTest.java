package com.dotcms.dotpubsub;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link JDBCPubSubImpl#dispatch(DotPubSubEvent, String, java.util.Collection)}: a
 * topic handler that throws on one notification must not stop the rest of a Postgres notification
 * batch from being delivered, cache invalidations included (issue #37876).
 *
 * @author hassandotcms
 */
public class JDBCPubSubImplDispatchTest {

    private static final String CHANNEL = "dotcache_topic";

    /** The topic subscribed to {@link #CHANNEL}; its handler throws on the first event it gets. */
    private static class FailsOnceTopic implements DotPubSubTopic {

        final List<String> delivered = new ArrayList<>();
        private boolean failed;

        @Override
        public Comparable getKey() {
            return CHANNEL;
        }

        @Override
        public void incrementReceivedCounters(final DotPubSubEvent event) {
            // not under test
        }

        @Override
        public void notify(final DotPubSubEvent event) {
            if (!failed) {
                failed = true;
                throw new IllegalStateException("handler failure");
            }
            delivered.add(event.getMessage());
        }
    }

    private static DotPubSubEvent event(final String message) {
        return new DotPubSubEvent.Builder().withTopic(CHANNEL).withType("INVAL").withMessage(message).build();
    }

    /**
     * Given : two notifications in one batch, and a topic whose handler throws on the first.
     * When  : they are dispatched one after the other, as the batch loop does.
     * Then  : the first failure doesn't escape, and the second notification is still delivered.
     */
    @Test
    public void handlerFailure_doesNotStopTheNextNotification() {
        final FailsOnceTopic topic = new FailsOnceTopic();

        JDBCPubSubImpl.dispatch(event("0:first"), CHANNEL, List.of(topic));
        JDBCPubSubImpl.dispatch(event("0:second"), CHANNEL, List.of(topic));

        assertEquals(List.of("0:second"), topic.delivered);
    }
}
