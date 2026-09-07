package com.dotcms.queue;

import com.dotcms.queue.provider.NoOpQueuePublisher;
import com.dotcms.queue.provider.SqsQueuePublisher;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class DotQueuePublisherLocatorTest {

    // --- resolve() tests: no static state, no order dependency ---

    @Test
    public void resolve_noop_returnsNoOpPublisher() {
        assertSame(NoOpQueuePublisher.INSTANCE, DotQueuePublisherLocator.resolve("noop"));
    }

    @Test
    public void resolve_noopCaseInsensitive() {
        assertSame(NoOpQueuePublisher.INSTANCE, DotQueuePublisherLocator.resolve("NOOP"));
    }

    @Test
    public void resolve_sqs_returnsSqsPublisher() {
        assertTrue(DotQueuePublisherLocator.resolve("sqs") instanceof SqsQueuePublisher);
    }

    @Test
    public void resolve_null_defaultsToNoOp() {
        assertSame(NoOpQueuePublisher.INSTANCE, DotQueuePublisherLocator.resolve(null));
    }

    @Test
    public void resolve_empty_defaultsToNoOp() {
        assertSame(NoOpQueuePublisher.INSTANCE, DotQueuePublisherLocator.resolve(""));
    }

    @Test
    public void resolve_whitespace_defaultsToNoOp() {
        assertSame(NoOpQueuePublisher.INSTANCE, DotQueuePublisherLocator.resolve("   "));
    }

    @Test
    public void resolve_trimmedNoop() {
        assertSame(NoOpQueuePublisher.INSTANCE, DotQueuePublisherLocator.resolve("  noop  "));
    }

    @Test(expected = DotQueueException.class)
    public void resolve_badClassName_throws() {
        DotQueuePublisherLocator.resolve("com.nonexistent.FakeProvider");
    }

    @Test(expected = DotQueueException.class)
    public void resolve_classDoesNotImplementInterface_throws() {
        DotQueuePublisherLocator.resolve(String.class.getName());
    }

    /**
     * Test fixture: valid class with no-arg constructor but not a DotQueuePublisher.
     * Used by {@link #resolve_classDoesNotImplementInterface_throws()}.
     * (String.class covers this case since it has a no-arg constructor.)
     */

    // --- Static locator test (default config) ---

    @Test
    public void get_returnsNonNull() {
        assertNotNull(DotQueuePublisherLocator.get());
    }
}
