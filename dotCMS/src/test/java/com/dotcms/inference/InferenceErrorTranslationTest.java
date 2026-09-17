package com.dotcms.inference;

import com.dotcms.inference.model.InferenceError;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.InvalidRequestException;
import dev.langchain4j.exception.ModelNotFoundException;
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

    /** Stands in for a provider message: endpoint, key and a fragment of the caller's prompt. */
    private static final String LEAKY =
            "api.openai.com rejected key sk-live-9f3c for org acme-corp while completing "
                    + "'draft the Q3 board memo'";

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
     * Given a provider that refused on an exhausted account
     * When the failure is translated
     * Then the caller gets a 400, not a retryable 502
     *
     * <p>The case that found this. An OpenRouter account out of credit answers 402 with "requires
     * more credits, or fewer max_tokens"; the provider library maps that to a non-retriable
     * exception, and this family used to answer 502. A 502 is the one thing that must not happen
     * there: it is the status a client's back-off reads as "try again", so a request that cannot
     * succeed until somebody adds credit gets retried on a schedule, burning a round trip each
     * time. The live log showed three attempts before the caller ever saw an answer.</p>
     */
    @Test
    public void test_exhaustedProviderAccount_isNotReportedAsRetryable() {
        final InferenceError error = InferenceError.fromProviderFailure(
                new InvalidRequestException(
                        "This request requires more credits, or fewer max_tokens. You requested "
                                + "up to 65536 tokens, but can only afford 30546"),
                UPSTREAM);

        assertEquals("A refusal a retry cannot fix must not carry a retryable status",
                400, error.httpStatus());
        assertFalse("The whole point is that a client stops retrying", error.isRetryable());
        assertFalse("The provider's wording names the account and the limits; it stays in the log",
                error.message().contains("65536"));
        assertFalse(error.message().contains("credits"));
    }

    /**
     * Given each kind of refusal the provider library classifies as non-retriable
     * When they are translated
     * Then none of them is reported as retryable
     *
     * <p>Asserted across the family rather than on the one that was reported, because they share
     * a parent in the library and the translation keys off that parent. A bad key and a model the
     * provider does not serve are as permanent as an empty account, and all three used to be
     * 502.</p>
     */
    @Test
    public void test_everyNonRetriableRefusal_isTerminal() {
        for (final RuntimeException refusal : new RuntimeException[]{
                new InvalidRequestException(LEAKY),
                new AuthenticationException(LEAKY),
                new ModelNotFoundException(LEAKY)}) {

            final InferenceError error = InferenceError.fromProviderFailure(refusal, UPSTREAM);
            assertFalse(refusal.getClass().getSimpleName()
                    + " cannot be fixed by retrying, so it must not be reported as retryable",
                    error.isRetryable());
            assertFalse(error.message().contains("sk-live-9f3c"));
        }
    }

    /**
     * Given a provider that rejected the request itself, delivered as a plain HTTP failure
     * When it is translated
     * Then the caller is told it is terminal, not asked to retry
     *
     * <p>The case that broke image generation. OpenAI answers {@code 400 unknown_parameter} when
     * sent a field the model does not accept, and that arrives as a bare {@link HttpException}
     * rather than one of the library's classified exception types — so it fell past the
     * non-retriable check and was reported as a retryable 502. A malformed request is as permanent
     * as an empty account: the same request will be rejected the same way forever.</p>
     *
     * <p>Checked across the 4xx range rather than on 400 alone, since the same reasoning covers a
     * model the provider does not serve and an account it will not bill.</p>
     */
    @Test
    public void test_providerRejectedTheRequest_isTerminal() {
        for (final int status : new int[]{400, 403, 404, 422}) {
            final InferenceError error = InferenceError.fromProviderFailure(
                    new HttpException(status, "{\"error\":{\"message\":\"Unknown parameter: "
                            + "'response_format'.\",\"code\":\"unknown_parameter\"}}"),
                    UPSTREAM);

            assertFalse("A provider " + status + " will be a " + status + " on retry too, so it "
                    + "must not carry a retryable status", error.isRetryable());
            assertFalse("The provider's own wording stays in the log",
                    error.message().contains("response_format"));
        }
    }

    /**
     * Given a provider 5xx delivered as a plain HTTP failure
     * When it is translated
     * Then it stays retryable
     *
     * <p>The boundary. Narrowing 4xx to terminal must not drag the 5xx range with it — a provider
     * having a bad minute is the case 502 and a client back-off exist for.</p>
     */
    @Test
    public void test_providerServerError_staysRetryable() {
        final InferenceError error =
                InferenceError.fromProviderFailure(new HttpException(503, LEAKY), UPSTREAM);

        assertEquals(502, error.httpStatus());
        assertTrue(error.isRetryable());
    }

    /**
     * Given a genuine upstream fault
     * When it is translated
     * Then it stays a retryable 502
     *
     * <p>The boundary in the other direction: the library calls this one retriable, and it is —
     * a provider having a bad minute is exactly what 502 and a client back-off are for. A fix
     * that turned every provider failure into a terminal 4xx would be as wrong as the bug.</p>
     */
    @Test
    public void test_genuineUpstreamFault_staysRetryable() {
        final InferenceError error =
                InferenceError.fromProviderFailure(new InternalServerException(LEAKY), UPSTREAM);

        assertEquals(502, error.httpStatus());
        assertTrue("A provider having a bad minute is worth retrying", error.isRetryable());
        assertEquals(UPSTREAM, error.message());
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
