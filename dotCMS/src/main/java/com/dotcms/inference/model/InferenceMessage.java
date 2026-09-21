package com.dotcms.inference.model;

import java.io.Serializable;
import java.util.List;

/**
 * One turn in an inference conversation.
 *
 * <p>An {@link Role#ASSISTANT} turn may carry {@code toolCalls} instead of {@code content} when
 * the model is asking for tools to be executed. A {@link Role#TOOL} turn carries the result and
 * must reference, through {@code toolCallId}, the identity of the call that requested it —
 * correlation is by identity, never by position.</p>
 *
 * @param role       who authored the turn; required
 * @param content    the text of the turn; may be null on an assistant turn carrying only tool calls
 * @param toolCalls  tool calls requested by the model; empty unless the role is ASSISTANT
 * @param toolCallId the call this turn answers; required when the role is TOOL
 * @param name       the tool that produced the result; optional, TOOL turns only
 */
public record InferenceMessage(Role role,
                               String content,
                               List<InferenceToolCall> toolCalls,
                               String toolCallId,
                               String name) implements Serializable {

    public InferenceMessage {
        if (role == null) {
            throw new IllegalArgumentException("InferenceMessage role is required");
        }
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        if (role == Role.TOOL && (toolCallId == null || toolCallId.isBlank())) {
            throw new IllegalArgumentException("A TOOL message requires the toolCallId it answers");
        }
        if (role != Role.ASSISTANT && !toolCalls.isEmpty()) {
            throw new IllegalArgumentException("Only an ASSISTANT message may carry tool calls");
        }
        if (content == null && toolCalls.isEmpty()) {
            throw new IllegalArgumentException(
                    "An InferenceMessage requires content unless it carries tool calls");
        }
    }

    /**
     * Creates a plain text turn.
     *
     * @param role    who authored the turn
     * @param content the text
     * @return a message with no tool calls
     */
    public static InferenceMessage of(final Role role, final String content) {
        return new InferenceMessage(role, content, List.of(), null, null);
    }

    /**
     * Creates an assistant turn that asks for tools to be executed.
     *
     * @param content   optional text accompanying the request
     * @param toolCalls the calls the model is requesting
     * @return an ASSISTANT message carrying tool calls
     */
    public static InferenceMessage ofToolCalls(final String content,
                                               final List<InferenceToolCall> toolCalls) {
        return new InferenceMessage(Role.ASSISTANT, content, toolCalls, null, null);
    }

    /**
     * Creates the result of executing a tool.
     *
     * @param toolCallId the call being answered
     * @param toolName   the tool that ran
     * @param content    the result
     * @return a TOOL message
     */
    public static InferenceMessage ofToolResult(final String toolCallId,
                                                final String toolName,
                                                final String content) {
        return new InferenceMessage(Role.TOOL, content, List.of(), toolCallId, toolName);
    }

    /**
     * @return whether this turn asks for one or more tools to be executed
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
