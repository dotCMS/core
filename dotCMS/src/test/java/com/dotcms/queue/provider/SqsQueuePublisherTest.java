package com.dotcms.queue.provider;

import com.dotcms.queue.DotQueueException;
import org.junit.Test;

/**
 * Unit tests for {@link SqsQueuePublisher}. These verify configuration-level
 * behavior without requiring an actual SQS connection.
 */
public class SqsQueuePublisherTest {

    @Test(expected = DotQueueException.class)
    public void publish_throwsWhenQueueUrlNotConfigured() {
        final SqsQueuePublisher publisher = new SqsQueuePublisher();
        publisher.publish("NONEXISTENT_QUEUE", "{\"test\":true}", null);
    }
}
