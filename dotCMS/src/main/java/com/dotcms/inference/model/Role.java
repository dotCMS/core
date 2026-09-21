package com.dotcms.inference.model;

/**
 * The author of a single turn in an inference conversation.
 *
 * <p>Deliberately free of any wire-format naming: the mapping between these constants and the
 * strings a client sends lives in the REST layer, not here. See {@link InferenceRequest} for why
 * the internal representation is kept independent of the format it is serialized to.</p>
 */
public enum Role {

    /** Instruction supplied by the caller, ahead of the conversation. */
    SYSTEM,
    /** A turn authored by the end user. */
    USER,
    /** A turn authored by the model; may carry tool calls instead of content. */
    ASSISTANT,
    /** The result of executing a tool the model asked for. */
    TOOL
}
