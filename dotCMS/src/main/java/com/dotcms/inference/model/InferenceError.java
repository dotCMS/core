package com.dotcms.inference.model;

import java.io.Serializable;

/**
 * A refusal, in the shape the REST layer serializes to the standard error object.
 *
 * <p>{@code httpStatus} is carried here but is deliberately <strong>not</strong> part of the
 * serialized body. Retryability is conveyed by the HTTP status — 429 and 5xx — because that is
 * what a standard client's back-off keys off; the standard error shape has no retryable field
 * and inventing one would make the payload stop deserializing into a client library's own
 * types.</p>
 *
 * @param type       the error family, e.g. {@code invalid_request_error}
 * @param message    a safe description; never the provider's raw envelope
 * @param param      the offending field where one can be named, otherwise null
 * @param httpStatus the status to respond with; not serialized into the body
 */
public record InferenceError(String type, String message, String param, int httpStatus)
        implements Serializable {

    public InferenceError {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("InferenceError type is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("InferenceError message is required");
        }
    }

    /**
     * @param message what was wrong
     * @param param   the offending field, or null
     * @return a 400 invalid-request error
     */
    public static InferenceError invalidRequest(final String message, final String param) {
        return new InferenceError("invalid_request_error", message, param, 400);
    }

    /**
     * @param model the model the caller asked for
     * @return a 404 error in the shape clients recognise as an unknown model
     */
    public static InferenceError noSuchModel(final String model) {
        return new InferenceError("invalid_request_error",
                "The model '" + model + "' is not configured for this site", "model", 404);
    }

    /**
     * @param message what failed upstream
     * @return a 502 error flagged retryable by its status
     */
    public static InferenceError upstream(final String message) {
        return new InferenceError("api_error", message, null, 502);
    }

    /**
     * Translates a provider failure into the status a standard client's back-off keys off.
     *
     * <p>FR-031. A provider refusing on rate limit and a provider genuinely broken are different
     * events with different right answers, and collapsing both into 502 tells a client to retry a
     * throttled request on the wrong schedule while telling it nothing about when to come back.
     * Rate limiting is 429; anything else upstream stays 502.</p>
     *
     * <p>The provider's own message is never carried out of here — it can hold its endpoint, its
     * account identifiers, and occasionally a fragment of the prompt. Only the status is taken
     * from it, which is why the caller supplies the wording.</p>
     *
     * <p>The cause chain is walked rather than the top exception inspected, because the provider
     * client wraps: a rate limit surfaces as its own type or as a generic HTTP failure carrying
     * 429, and by the time a retrying fallback chain has rethrown it, either can be two or three
     * causes down.</p>
     *
     * @param failure         the exception the provider client raised
     * @param upstreamMessage the wording to return for a non-rate-limit failure
     * @return the error to answer with
     */
    public static InferenceError fromProviderFailure(final Throwable failure,
                                                     final String upstreamMessage) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof dev.langchain4j.exception.RateLimitException) {
                return rateLimited();
            }
            if (current instanceof dev.langchain4j.exception.HttpException httpFailure
                    && httpFailure.statusCode() == 429) {
                return rateLimited();
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return upstream(upstreamMessage);
    }

    /**
     * @return the standard rate-limit refusal, which a client reads as "retry later"
     */
    public static InferenceError rateLimited() {
        return new InferenceError("rate_limit_error",
                "The model provider is rate limiting this site's requests", null, 429);
    }

    /** @return whether a standard client should retry, as implied by the status */
    public boolean isRetryable() {
        return httpStatus == 429 || httpStatus >= 500;
    }
}
