package com.dotcms.queue.provider;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;

public class NoOpQueuePublisherTest {

    @Test
    public void publish_doesNotThrow() {
        NoOpQueuePublisher.INSTANCE.publish("test-queue", "{}", Map.of("key", "value"));
    }

    @Test
    public void isAvailable_returnsFalse() {
        assertFalse(NoOpQueuePublisher.INSTANCE.isAvailable("ANY_QUEUE"));
    }
}
