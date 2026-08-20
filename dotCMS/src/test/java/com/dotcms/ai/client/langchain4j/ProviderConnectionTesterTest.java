package com.dotcms.ai.client.langchain4j;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for {@link ProviderConnectionTester#friendlyMessage} (message truncation) and
 * {@link ProviderConnectionTester#withDefaultTimeoutIfUnset} (bounding an otherwise-unbounded test
 * call when the posted config omits {@code timeout}).
 */
public class ProviderConnectionTesterTest {

    /**
     * Given an exception with a short message,
     * When friendlyMessage is called,
     * Then the message is returned unchanged.
     */
    @Test
    public void test_friendlyMessage_shortMessage_returnsUnchanged() {
        final String message = "Invalid API key provided";

        assertEquals(message, ProviderConnectionTester.friendlyMessage(new RuntimeException(message)));
    }

    /**
     * Given an exception with a null message,
     * When friendlyMessage is called,
     * Then the exception's simple class name is returned.
     */
    @Test
    public void test_friendlyMessage_nullMessage_returnsClassName() {
        assertEquals("RuntimeException", ProviderConnectionTester.friendlyMessage(new RuntimeException()));
    }

    /**
     * Given an exception with a blank (whitespace-only) message,
     * When friendlyMessage is called,
     * Then the exception's simple class name is returned.
     */
    @Test
    public void test_friendlyMessage_blankMessage_returnsClassName() {
        assertEquals("IllegalStateException",
                ProviderConnectionTester.friendlyMessage(new IllegalStateException("   ")));
    }

    /**
     * Given an exception whose message is longer than the cap,
     * When friendlyMessage is called,
     * Then the result is truncated to the cap length plus the ellipsis suffix.
     */
    @Test
    public void test_friendlyMessage_longMessage_truncatedWithEllipsis() {
        final String longMessage = "x".repeat(500);

        final String result = ProviderConnectionTester.friendlyMessage(new RuntimeException(longMessage));

        assertTrue(result.endsWith("…"));
        // 200 chars kept + 1 ellipsis char
        assertEquals(201, result.length());
    }

    /**
     * Given an exception message at exactly the cap length,
     * When friendlyMessage is called,
     * Then it is returned unchanged, with no truncation applied.
     */
    @Test
    public void test_friendlyMessage_exactlyAtCap_returnsUnchanged() {
        final String message = "x".repeat(200);

        assertEquals(message, ProviderConnectionTester.friendlyMessage(new RuntimeException(message)));
    }

    /**
     * Given a message one character over the cap,
     * When friendlyMessage is called,
     * Then it is truncated.
     */
    @Test
    public void test_friendlyMessage_oneOverCap_truncated() {
        final String message = "x".repeat(201);

        final String result = ProviderConnectionTester.friendlyMessage(new RuntimeException(message));

        assertFalse(result.equals(message));
        assertTrue(result.endsWith("…"));
    }

    /**
     * Given an exception message containing newlines and repeated whitespace (typical of a
     * pretty-printed JSON error body),
     * When friendlyMessage is called,
     * Then the whitespace is collapsed into single spaces.
     */
    @Test
    public void test_friendlyMessage_multilineMessage_whitespaceCollapsed() {
        final String message = "Error occurred:\n\n  {\n    \"code\": 401,\n    \"message\": \"bad key\"\n  }";

        final String result = ProviderConnectionTester.friendlyMessage(new RuntimeException(message));

        assertFalse(result.contains("\n"));
        assertTrue(result.contains("Error occurred: { \"code\": 401, \"message\": \"bad key\" }"));
    }

    /**
     * Given an exception message with leading/trailing whitespace,
     * When friendlyMessage is called,
     * Then the result is trimmed.
     */
    @Test
    public void test_friendlyMessage_leadingTrailingWhitespace_trimmed() {
        final String message = "   Invalid credentials   ";

        assertEquals("Invalid credentials", ProviderConnectionTester.friendlyMessage(new RuntimeException(message)));
    }

    // -------------------------------------------------------------------------
    // withDefaultTimeoutIfUnset
    // -------------------------------------------------------------------------

    /**
     * Given a config with no timeout set,
     * When withDefaultTimeoutIfUnset is called,
     * Then the default test timeout is applied.
     */
    @Test
    public void test_withDefaultTimeoutIfUnset_noTimeoutSet_appliesDefault() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai").apiKey("test-key").model("gpt-4o").build();

        final ProviderConfig result = ProviderConnectionTester.withDefaultTimeoutIfUnset(config);

        assertEquals(Integer.valueOf(10), result.timeout());
    }

    /**
     * Given a config that already sets a timeout,
     * When withDefaultTimeoutIfUnset is called,
     * Then the caller's timeout is preserved unchanged.
     */
    @Test
    public void test_withDefaultTimeoutIfUnset_timeoutAlreadySet_preservesCallerValue() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai").apiKey("test-key").model("gpt-4o").timeout(45).build();

        final ProviderConfig result = ProviderConnectionTester.withDefaultTimeoutIfUnset(config);

        assertEquals(Integer.valueOf(45), result.timeout());
    }

    /**
     * Given a config with no timeout set,
     * When withDefaultTimeoutIfUnset is called,
     * Then every other field is preserved unchanged — only timeout is added.
     */
    @Test
    public void test_withDefaultTimeoutIfUnset_preservesOtherFields() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai").apiKey("test-key").model("gpt-4o").temperature(0.5).build();

        final ProviderConfig result = ProviderConnectionTester.withDefaultTimeoutIfUnset(config);

        assertEquals("openai", result.provider());
        assertEquals("test-key", result.apiKey());
        assertEquals("gpt-4o", result.model());
        assertEquals(Double.valueOf(0.5), result.temperature());
    }

}
