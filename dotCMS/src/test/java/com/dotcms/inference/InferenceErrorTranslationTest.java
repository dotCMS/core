package com.dotcms.inference;

import com.dotcms.inference.model.InferenceError;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RateLimitException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers FR-031's translation of a provider failure into the status a client backs off on.
 *
 * <p>The distinction this pins is not cosmetic. A standard client decides what to do next from the
 * HTTP status alone — the error shape carries no retryable field, and inventing one would break the
 * no-adapter promise of SC-008 — so answering 502 to a throttled request tells the client the
 * provider is broken rather than busy, and it retries on the wrong schedule. Every assertion here
 * is about that one decision.</p>
 */
public class InferenceErrorTranslationTest {

    private static final String UPSTREAM = "The model provider failed to complete the request";

    /**
     * Given a provider that refused because the site is over its rate limit
     * When the failure is translated
     * Then the caller is told 429, in the standard rate-limit shape
     */
    @Test
    public void test_rateLimitFailure_becomesA429() {
        final InferenceError error = InferenceError.fromProviderFailure(
                new RateLimitException("slow down"), UPSTREAM);

        assertEquals("A throttled request is 429, which is what a client's back-off keys off",
                429, error.httpStatus());
        assertEquals("rate_limit_error", error.type());
        assertTrue(error.isRetryable());
    }

    /**
     * Given a provider that answered with HTTP 429 rather than a typed exception
     * When the failure is translated
     * Then it is still a 429
     *
     * <p>Both shapes occur: the provider client raises its own type for some providers and a plain
     * HTTP failure carrying the status for others, and a translation that recognised only the typed
     * one would be correct for whichever provider it was written against and wrong for the rest.</p>
     */
    @Test
    public void test_http429_becomesA429() {
        final InferenceError error = InferenceError.fromProviderFailure(
                new HttpException(429, "Too Many Requests"), UPSTREAM);

        assertEquals(429, error.httpStatus());
        assertEquals("rate_limit_error", error.type());
    }

    /**
     * Given a rate limit buried under the wrappers a retrying fallback chain adds
     * When the failure is translated
     * Then it is still a 429
     *
     * <p>This is the case that actually reaches production. The fallback chain catches, logs and
     * rethrows as it walks a site's configured models, so by the time the resource sees the failure
     * the rate limit is several causes down. A translation that inspected only the top exception
     * would pass every test above and still answer 502 to every real throttled request.</p>
     */
    @Test
    public void test_rateLimitWrappedByTheFallbackChain_isStillA429() {
        final InferenceError error = InferenceError.fromProviderFailure(
                new RuntimeException("image model 'dall-e-3' failed",
                        new IllegalStateException("retrying",
                                new RateLimitException("slow down"))),
                UPSTREAM);

        assertEquals("A rate limit the fallback chain wrapped is still a rate limit",
                429, error.httpStatus());
    }

    /**
     * Given a provider that genuinely failed, and a self-referencing cause chain
     * When the failure is translated
     * Then it is 502, and the translation terminates
     *
     * <p>The 5xx half of FR-031, plus the loop guard: a cause chain that points at itself is rare
     * but legal, and a walk without the guard hangs the request thread rather than failing it.</p>
     */
    @Test
    public void test_ordinaryFailure_becomes502_andSelfReferencingCauseTerminates() {
        final InferenceError plain = InferenceError.fromProviderFailure(
                new HttpException(500, "Internal Server Error"), UPSTREAM);
        assertEquals(502, plain.httpStatus());
        assertEquals("api_error", plain.type());
        assertEquals(UPSTREAM, plain.message());

        final RuntimeException selfReferencing = new RuntimeException("broken") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };
        assertEquals(502, InferenceError.fromProviderFailure(selfReferencing, UPSTREAM).httpStatus());
    }

    /**
     * Given a provider failure of any kind
     * When it is translated
     * Then the provider's own wording never reaches the caller
     *
     * <p>FR-031 and FR-036 together: a provider message can carry its endpoint, account
     * identifiers, or a fragment of the prompt, so only the status is taken from it.</p>
     */
    @Test
    public void test_providerWordingIsNeverReturned() {
        final String leaky = "api.openai.com rejected key sk-secret for org acme: 'draft a memo'";

        final InferenceError rateLimited = InferenceError.fromProviderFailure(
                new RateLimitException(leaky), UPSTREAM);
        final InferenceError failed = InferenceError.fromProviderFailure(
                new HttpException(503, leaky), UPSTREAM);

        for (final InferenceError error : new InferenceError[]{rateLimited, failed}) {
            assertFalse("The provider's own message must never be handed to a caller",
                    error.message().contains("sk-secret"));
            assertFalse(error.message().contains("api.openai.com"));
            assertFalse(error.message().contains("draft a memo"));
        }
    }
}
