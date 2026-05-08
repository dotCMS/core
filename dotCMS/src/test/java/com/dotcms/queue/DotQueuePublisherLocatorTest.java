package com.dotcms.queue;

import com.dotcms.queue.provider.NoOpQueuePublisher;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the locator returns a valid publisher. The default (no config) should
 * be the NoOp implementation.
 */
public class DotQueuePublisherLocatorTest {

    @Test
    public void get_returnsNonNull() {
        assertNotNull(DotQueuePublisherLocator.get());
    }

    @Test
    public void defaultProvider_isNoOp() {
        assertTrue(
                "Default provider should be NoOpQueuePublisher",
                DotQueuePublisherLocator.get() instanceof NoOpQueuePublisher);
    }
}
