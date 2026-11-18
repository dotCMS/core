package com.dotcms.inference.model;

/**
 * Why a completion stopped.
 *
 * <p>{@link #ERROR} has no equivalent in the chat-completions wire format, which has no finish
 * reason meaning "this failed". It exists here because a stream that fails after it has begun
 * must be distinguishable from one that finished, and the serializer turns it into an error
 * event rather than a finish reason.</p>
 */
public enum FinishReason {

    /** The model finished naturally. */
    STOP,
    /** The model hit a token ceiling. */
    LENGTH,
    /** The model is asking for one or more tools to be executed. */
    TOOL_CALLS,
    /** The provider suppressed the response. */
    CONTENT_FILTER,
    /** The exchange failed. Never serialized as a finish reason. */
    ERROR
}
