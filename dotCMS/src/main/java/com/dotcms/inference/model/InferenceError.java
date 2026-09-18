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
    public static InferenceError noSuchModel(final String model, final String capability) {
        // Naming the capability rather than saying "not configured for this site", which is true
        // of the site's chat models only in the sense that matters and false in the sense a caller
        // reads. The model listing returns every model the site has configured across all three
        // capabilities, because the adopted format has nowhere to record what a model is for — so
        // a caller can see an id in the list and then be told it is not configured, with no way to
        // resolve the contradiction from the API alone. This refusal is the only place that
        // information can live.
        return new InferenceError("invalid_request_error",
                "The model '" + model + "' is not configured for " + capability + " on this site",
                "model", 404);
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
     * <p>Three outcomes, not two, because "the provider said no" splits along a line a client
     * acts on. A rate limit is temporary: 429, come back later. A refusal the provider will
     * repeat — an exhausted account, a rejected key, a model the provider does not serve — is
     * permanent until somebody changes something, and answering 502 there tells a standard
     * client's back-off to retry a request that cannot ever succeed. Everything left is a
     * genuine upstream fault, which is what 502 is for.</p>
     *
     * <p>Permanence is read from the provider library's own classification rather than from a
     * list of exception types or status codes: it already sorts its failures into retriable and
     * non-retriable, and keying off that means a type added in a later release is classified
     * correctly without this method being touched.</p>
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
            if (current instanceof dev.langchain4j.exception.HttpException httpFailure) {
                if (httpFailure.statusCode() == 429) {
                    return rateLimited();
                }
                // Any other 4xx is the provider saying the request itself is wrong — a parameter
                // it does not accept, a model it does not serve, an account it will not bill. It
                // will say the same thing to an identical retry, so it must not leave here with a
                // retryable status. This arrives as a bare HttpException rather than one of the
                // library's classified types, which is how a permanently malformed request used
                // to be reported as a temporary upstream fault.
                if (httpFailure.statusCode() >= 400 && httpFailure.statusCode() < 500) {
                    return providerRefused();
                }
            }
            if (current instanceof dev.langchain4j.exception.NonRetriableException) {
                return providerRefused();
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
    /**
     * @return the refusal for a provider failure that a retry cannot fix
     */
    public static InferenceError providerRefused() {
        // 400 rather than 502, because the status is the only thing carrying retryability and a
        // 5xx would keep a client coming back. It is an imperfect fit — the caller has usually
        // done nothing wrong, and the thing to fix is the site's provider account or
        // configuration — so the message says where to look rather than implying the request was
        // malformed. The provider's own wording, which names the account and sometimes the
        // prompt, stays in the log.
        return new InferenceError("invalid_request_error",
                "The model provider refused this request, and will refuse it again on retry. "
                        + "This is a provider account or site configuration problem rather than "
                        + "a problem with the request; the details are in the server log.",
                null, 400);
    }

    public static InferenceError rateLimited() {
        return new InferenceError("rate_limit_error",
                "The model provider is rate limiting this site's requests", null, 429);
    }

    /** @return whether a standard client should retry, as implied by the status */
    public boolean isRetryable() {
        return httpStatus == 429 || httpStatus >= 500;
    }
}
