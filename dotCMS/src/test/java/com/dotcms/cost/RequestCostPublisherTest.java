package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        Config.setProperty("REQUEST_COST_PUSH_TIMEOUT_MS", null);
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

    /**
     * Regression: a token that is only CRLF / whitespace must NOT activate the publisher.
     * If the gate used the raw token, isSet would return true, the publisher would activate,
     * and {@code post()}'s sanitizer would strip the token to empty — resulting in an
     * unauthenticated POST to the collector. Worse than staying disabled.
     */
    @Test
    public void test_isEnabled_returnsFalse_whenTokenIsOnlyWhitespaceOrCrlf() {
        // Given
        Config.setProperty("REQUEST_COST_PUSH_URL", "https://example.com/cost");
        Config.setProperty("REQUEST_COST_PUSH_TOKEN", "  \r\n  ");

        // When / Then
        assertFalse("whitespace/CRLF-only token must not activate the publisher",
                publisher.isEnabled());
    }

    @Test
    public void test_publish_whenDisabled_isANoOp() {
        // Given — both keys absent

        // When / Then — must not throw, must not submit anything to the executor
        publisher.publish(anySnapshot());
    }

    /**
     * Verifies that {@link Config#setProperty(String, Object) Config.setProperty(key, null)}
     * actually unsets the property — i.e. the enable gate sees null, not the literal string
     * {@code "null"}. If this ever regresses, {@link UtilMethods#isSet(Object)} would return true
     * for {@code "null"} and the gate would silently flip on.
     */
    @Test
    public void test_clearConfig_actuallyRemovesProperties() {
        // Given — both keys set then cleared
        Config.setProperty("REQUEST_COST_PUSH_URL", "https://example.com/cost");
        Config.setProperty("REQUEST_COST_PUSH_TOKEN", "secret");
        Config.setProperty("REQUEST_COST_PUSH_URL", null);
        Config.setProperty("REQUEST_COST_PUSH_TOKEN", null);

        // When
        final String url = Config.getStringProperty("REQUEST_COST_PUSH_URL", null);
        final String token = Config.getStringProperty("REQUEST_COST_PUSH_TOKEN", null);

        // Then — must be null, not the literal string "null"
        assertNull("URL must be unset, not stringified", url);
        assertNull("token must be unset, not stringified", token);
        assertFalse("publisher must observe the cleared state", publisher.isEnabled());
    }

    @Test
    public void test_sanitizeUrlForLog_stripsUserinfo() {
        // Given / When
        final String sanitized = RequestCostPublisher.sanitizeUrlForLog(
                "https://user:secret@host.example.com/cost?x=1");

        // Then — no credentials leak into log lines
        assertEquals("https://host.example.com/cost?x=1", sanitized);
    }

    @Test
    public void test_sanitizeUrlForLog_passesPlainUrlThrough() {
        // Given / When
        final String sanitized = RequestCostPublisher.sanitizeUrlForLog(
                "https://host.example.com/cost");

        // Then
        assertEquals("https://host.example.com/cost", sanitized);
    }

    @Test
    public void test_sanitizeHeaderValue_stripsCrlfAndTrims() {
        // CRLF in a header value would otherwise enable HTTP header injection
        assertEquals("legit-token", RequestCostPublisher.sanitizeHeaderValue("legit-token"));
        assertEquals("legit-token", RequestCostPublisher.sanitizeHeaderValue("  legit-token  "));
        assertEquals("evilXInjected: value",
                RequestCostPublisher.sanitizeHeaderValue("evil\r\nXInjected: value"));
        assertEquals("token", RequestCostPublisher.sanitizeHeaderValue("token\n"));
        assertNull(RequestCostPublisher.sanitizeHeaderValue(null));
    }
}
