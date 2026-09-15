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

    /** @return whether a standard client should retry, as implied by the status */
    public boolean isRetryable() {
        return httpStatus == 429 || httpStatus >= 500;
    }
}
