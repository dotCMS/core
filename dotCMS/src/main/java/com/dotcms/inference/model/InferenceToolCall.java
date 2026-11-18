package com.dotcms.inference.model;

import java.io.Serializable;

/**
 * A request from the model to execute one declared tool.
 *
 * <p>This type is the reason the internal representation exists. Identity is carried in
 * {@code id}, which comes from the provider and is what a later {@link Role#TOOL} turn
 * correlates against. It is never derived from {@code index}: the chat-completions streaming
 * format happens to fragment tool calls and key the fragments by position, but that is a
 * property of one serialization, and rebuilding identity from it would bake that format into
 * the model. {@code index} is retained only so a serializer can reproduce it.</p>
 *
 * @param id        provider-assigned identity; required
 * @param name      the tool to execute; required
 * @param arguments the model's arguments as JSON text, passed through unparsed
 * @param index     position within the assistant turn; serialization detail only
 */
public record InferenceToolCall(String id, String name, String arguments, int index)
        implements Serializable {

    public InferenceToolCall {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("InferenceToolCall id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("InferenceToolCall name is required");
        }
        arguments = arguments == null ? "" : arguments;
    }

    /**
     * Creates a tool call at the head position, for callers that do not track ordering.
     *
     * @param id        provider-assigned identity
     * @param name      the tool to execute
     * @param arguments the model's arguments as JSON text
     * @return a tool call at index {@code 0}
     */
    public static InferenceToolCall of(final String id, final String name, final String arguments) {
        return new InferenceToolCall(id, name, arguments, 0);
    }
}
