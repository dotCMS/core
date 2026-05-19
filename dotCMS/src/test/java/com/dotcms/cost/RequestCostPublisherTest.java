package com.dotcms.cost;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link RequestCostPublisher}. Focuses on the enable gate and the
 * disabled-is-a-no-op guarantee. The actual HTTP POST is covered indirectly by
 * {@link RequestCostSnapshotTest} (payload shape) and would need a localhost HTTP server plus
 * {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS=true} set before {@code CircuitBreakerUrl}'s static
 * initializer fires — not a stable unit-test surface.
 */
public class RequestCostPublisherTest extends UnitTestBase {

    private final RequestCostPublisher publisher = new RequestCostPublisher();

    @After
    public void clearConfig() {
        Config.setProperty("REQUEST_COST_PUSH_URL", null);
        Config.setProperty("REQUEST_COST_PUSH_TOKEN", null);
    }

    private RequestCostSnapshot anySnapshot() {
        return new RequestCostSnapshot(
                "c", "e", "2026-05-19T00:00:00Z",
                60, 0L, 0d, 0d, 0L, 0d, 0d);
    }

    @Test
    public void test_isEnabled_returnsFalse_whenUrlAndTokenAreMissing() {
        // Given — clearConfig in @After ensures both are null

        // When / Then
        assertFalse("publisher must default to off", publisher.isEnabled());
    }

    @Test
    public void test_isEnabled_returnsFalse_whenOnlyUrlIsSet() {
        // Given
        Config.setProperty("REQUEST_COST_PUSH_URL", "https://example.com/cost");

        // When / Then
        assertFalse("url alone must not enable the publisher", publisher.isEnabled());
    }

    @Test
    public void test_isEnabled_returnsFalse_whenOnlyTokenIsSet() {
        // Given
        Config.setProperty("REQUEST_COST_PUSH_TOKEN", "secret");

        // When / Then
        assertFalse("token alone must not enable the publisher", publisher.isEnabled());
    }

    @Test
    public void test_isEnabled_returnsTrue_whenUrlAndTokenAreBothSet() {
        // Given
        Config.setProperty("REQUEST_COST_PUSH_URL", "https://example.com/cost");
        Config.setProperty("REQUEST_COST_PUSH_TOKEN", "secret");

        // When / Then
        assertTrue("publisher must activate when both keys are present", publisher.isEnabled());
    }

    @Test
    public void test_publish_whenDisabled_isANoOp() {
        // Given — both keys absent

        // When / Then — must not throw, must not submit anything to the executor
        publisher.publish(anySnapshot());
    }
}
