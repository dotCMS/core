package com.dotcms.ai.client.langchain4j;

import com.dotcms.inference.model.CallerSafeException;
import com.dotcms.inference.model.InferenceError;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RateLimitException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the one decision that separates a message a caller may read from one they may not.
 *
 * <p>Most failures reaching the stream's error translator are {@link IllegalArgumentException}s,
 * and they come from two places that look identical at the catch site. Some carry a sentence
 * dotCMS composed about the site's configuration — the caller needs that to fix the problem.
 * Others are the fallback chain's wrapper around a failed model initialisation, which appends the
 * provider client's own message, and that can hold the provider's endpoint, its account
 * identifiers, or a fragment of the prompt.</p>
 *
 * <p>Passing the message through on type alone returned both. These tests pin the distinction by
 * intent — {@link CallerSafeException} is returned, everything else is not — so a future
 * contributor who throws a bare {@code IllegalArgumentException} carrying provider text gets a
 * generic sentence rather than a leak.</p>
 */
public class InferenceErrorLeakTest {

    /** What a provider client's message can look like, and must never reach a caller. */
    private static final String LEAKY =
            "api.openai.com rejected key sk-live-9f3c for org acme-corp while completing "
                    + "'draft the Q3 board memo'";

    /**
     * Given a model initialisation that failed with the provider's own message attached
     * When the failure is translated for the caller
     * Then the caller is told the request could not be served, and none of the provider's text
     * reaches them
     *
     * <p>This is the shape the fallback chain actually produces: it catches whatever the provider
     * client threw and rethrows an {@code IllegalArgumentException} whose message is dotCMS's
     * prefix followed by the provider's. Before this test, that whole string was returned.</p>
     */
    @Test
    public void test_wrappedProviderMessage_isNotReturnedToTheCaller() {
        final IllegalArgumentException asTheFallbackChainThrowsIt = new IllegalArgumentException(
                "Failed to initialize chat model 'gpt-4o-mini': " + LEAKY,
                new RuntimeException(LEAKY));

        final InferenceError error =
                InferenceAIClient.toInferenceError(asTheFallbackChainThrowsIt);

        assertEquals(400, error.httpStatus());
        assertEquals("The request could not be served with this site's configuration",
                error.message());
        assertFalse("The provider's endpoint must not reach the caller",
                error.message().contains("api.openai.com"));
        assertFalse("An API key must never reach the caller",
                error.message().contains("sk-live-9f3c"));
        assertFalse("The caller's own prompt must not be quoted back through an error",
                error.message().contains("board memo"));
        assertFalse("Even the internal wrapper text is withheld; it names nothing actionable",
                error.message().contains("Failed to initialize"));
    }

    /**
     * Given a configuration problem dotCMS itself described
     * When the failure is translated
     * Then the caller gets that sentence, because it is the one thing that says what to fix
     *
     * <p>The other half of the rule, and the reason this is a type distinction rather than a
     * blanket suppression: answering every configuration problem with "could not be served" would
     * be safe and useless, and a site administrator would have nothing to act on.</p>
     */
    @Test
    public void test_messageDotcmsWrote_isReturned() {
        final InferenceError error = InferenceAIClient.toInferenceError(
                new CallerSafeException("No model configured in providerConfig.chat — set 'model'"));

        assertEquals(400, error.httpStatus());
        assertEquals("No model configured in providerConfig.chat — set 'model'", error.message());
    }

    /**
     * Given a provider failure that is not an argument problem at all
     * When it is translated
     * Then it stays on the upstream path, rate limits included
     *
     * <p>Guards against the fix above swallowing the FR-031 translation: the new branch sits in
     * front of it, and a version that matched too eagerly would turn every 429 into a 400 and
     * stop clients backing off.</p>
     */
    @Test
    public void test_upstreamFailures_stillTranslateByStatus() {
        assertEquals("A rate limit must still be a 429",
                429, InferenceAIClient.toInferenceError(new RateLimitException(LEAKY)).httpStatus());

        final InferenceError upstream =
                InferenceAIClient.toInferenceError(new HttpException(503, LEAKY));
        assertEquals(502, upstream.httpStatus());
        assertTrue("An upstream failure keeps the api_error type",
                "api_error".equals(upstream.type()));
        assertFalse(upstream.message().contains("sk-live-9f3c"));
    }

    /**
     * Given a caller-safe exception carrying no message
     * When it is translated
     * Then the caller still gets a usable sentence rather than a blank one
     */
    @Test
    public void test_blankSafeMessage_fallsBackToTheGenericSentence() {
        final InferenceError error = InferenceAIClient.toInferenceError(new CallerSafeException("  "));

        assertEquals(400, error.httpStatus());
        assertEquals("The request could not be served with this site's configuration",
                error.message());
    }
}
