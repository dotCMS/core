package com.dotcms.inference.model;

import java.io.Serializable;

/**
 * A completed, non-streamed answer.
 *
 * <p>{@code model} is the model that actually served the request, which is not necessarily the
 * one asked for: a site's fallback chain may have moved on to a later entry. Reporting the one
 * that ran, rather than the one requested, is what lets a caller tell that a fallback
 * happened.</p>
 *
 * @param id                 identity for this response
 * @param model              the model that served it, after any fallback
 * @param createdEpochSeconds when it was produced
 * @param message            the assistant turn; may carry tool calls instead of content
 * @param finishReason       why generation stopped
 * @param usage              tokens consumed, {@link InferenceUsage#UNREPORTED} if the provider was silent
 */
public record InferenceResponse(String id,
                                String model,
                                long createdEpochSeconds,
                                InferenceMessage message,
                                FinishReason finishReason,
                                InferenceUsage usage) implements Serializable {

    public InferenceResponse {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("InferenceResponse id is required");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("InferenceResponse model is required");
        }
        if (message == null) {
            throw new IllegalArgumentException("InferenceResponse message is required");
        }
        if (finishReason == null) {
            throw new IllegalArgumentException("InferenceResponse finishReason is required");
        }
        usage = usage == null ? InferenceUsage.UNREPORTED : usage;
    }
}
