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
        new SqsQueuePublisher().publish("NONEXISTENT_QUEUE", "{\"test\":true}", null);
    }

    @Test(expected = DotQueueException.class)
    public void publish_throwsWhenQueueNameIsNull() {
        new SqsQueuePublisher().publish(null, "{}", null);
    }

    @Test(expected = DotQueueException.class)
    public void publish_throwsWhenMessageBodyIsNull() {
        new SqsQueuePublisher().publish("SOME_QUEUE", null, null);
    }

    @Test(expected = DotQueueException.class)
    public void publish_throwsWhenQueueNameIsEmpty() {
        new SqsQueuePublisher().publish("", "{}", null);
    }
}
