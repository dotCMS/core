package com.dotcms.inference.rest.mapper;

import com.dotcms.inference.model.FinishReason;
import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Renders internal {@link InferenceStreamEvent}s as the server-sent events a standard
 * chat-completions client reads from {@code /api/inference/v1/chat/completions} when
 * {@code "stream": true}.
 *
 * <p>Byte compatibility with the external standard is the whole point of this endpoint family, so
 * every frame is {@code data: } + one compact JSON object + a blank line. The blank line is not
 * decoration: an SSE reader only dispatches an event once it sees it, so a frame without it leaves
 * the client blocked holding a chunk it already received.</p>
 *
 * <p>The render is an exhaustive switch over the sealed {@link InferenceStreamEvent}. That is
 * deliberate: a sixth variant added later must fail to compile here rather than silently fall
 * through to a default branch and reach the wire as something a client cannot read.</p>
 *
 * <p>The JSON is built with Jackson rather than concatenated, because the shapes nest and the
 * values — answer text and tool-call argument fragments — are attacker-influenced strings that
 * must be escaped correctly.</p>
 */
public final class SseSerializer {

    /**
     * The terminal marker, exactly as it goes on the wire. Clients detect end-of-stream by matching
     * these bytes, so it is a constant rather than something assembled per call.
     */
    public static final String DONE_MARKER = "data: [DONE]\n\n";

    /** The {@code object} discriminator every streamed chunk carries. */
    public static final String CHUNK_OBJECT = "chat.completion.chunk";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DATA_PREFIX = "data: ";
    private static final String FRAME_TERMINATOR = "\n\n";
    private static final String FUNCTION_TYPE = "function";

    private SseSerializer() {
        throw new AssertionError("SseSerializer is a utility class and is not instantiable");
    }

    /**
     * Renders one stream event as a complete server-sent event frame.
     *
     * <p>The completion id, model and creation time are repeated on every chunk of one completion
     * and never change mid-stream: a client correlates the chunks it is stitching together by that
     * id, so re-deriving them per event would break the correlation.</p>
     *
     * @param event               the event to render
     * @param completionId        the id shared by every chunk of this completion
     * @param model               the model that served the completion
     * @param createdEpochSeconds when the completion was created, in epoch seconds
     * @return the frame, {@code data: } prefix and blank-line terminator included
     */
    public static String toFrame(final InferenceStreamEvent event,
                                 final String completionId,
                                 final String model,
                                 final long createdEpochSeconds) {

        final ObjectNode payload = switch (event) {

            case InferenceStreamEvent.ContentDelta contentDelta ->
                    contentChunk(contentDelta, completionId, model, createdEpochSeconds);

            case InferenceStreamEvent.ToolCallDelta toolCallDelta ->
                    toolCallChunk(toolCallDelta, completionId, model, createdEpochSeconds);

            case InferenceStreamEvent.Finish finish ->
                    finishChunk(finish, completionId, model, createdEpochSeconds);

            case InferenceStreamEvent.Usage usage ->
                    usageChunk(usage, completionId, model, createdEpochSeconds);

            case InferenceStreamEvent.Error error -> errorPayload(error);
        };

        return DATA_PREFIX + payload.toString() + FRAME_TERMINATOR;
    }

    /**
     * Answers whether the terminal {@code [DONE]} marker may follow this event.
     *
     * <p>It may, except after {@link InferenceStreamEvent.Error}. Once the first frame is written
     * the HTTP status is already on the wire and can no longer carry a failure, so withholding the
     * marker is the only thing that stops a client which does not parse the error frame from
     * reading a truncated answer as a complete one. Every other variant answers {@code true}; the
     * question is only ever asked of the last event a stream produced.</p>
     *
     * @param event the last event the stream produced
     * @return whether to close the stream with {@link #DONE_MARKER}
     */
    public static boolean shouldWriteDoneMarker(final InferenceStreamEvent event) {

        return switch (event) {
            case InferenceStreamEvent.Error ignored -> false;
            case InferenceStreamEvent.ContentDelta ignored -> true;
            case InferenceStreamEvent.ToolCallDelta ignored -> true;
            case InferenceStreamEvent.Finish ignored -> true;
            case InferenceStreamEvent.Usage ignored -> true;
        };
    }

    /**
     * Builds a chunk carrying one fragment of the answer's text.
     *
     * @param contentDelta        the fragment
     * @param completionId        the id shared by every chunk of this completion
     * @param model               the model that served the completion
     * @param createdEpochSeconds when the completion was created, in epoch seconds
     * @return the chunk
     */
    private static ObjectNode contentChunk(final InferenceStreamEvent.ContentDelta contentDelta,
                                           final String completionId,
                                           final String model,
                                           final long createdEpochSeconds) {

        final ObjectNode chunk = envelope(completionId, model, createdEpochSeconds);
        final ObjectNode choice = firstChoiceOf(chunk);

        choice.putObject("delta").put("content", contentDelta.text());
        choice.putNull("finish_reason");

        return chunk;
    }

    /**
     * Builds a chunk carrying one fragment of one tool call.
     *
     * <p>Only the first fragment of a call carries its {@code id}, its {@code type} and the tool's
     * name; a continuation fragment emits neither key <strong>at all</strong>, not even as JSON
     * null, because a null id reads to a client as a different call starting. {@code index} is the
     * only thing tying a continuation fragment back to the call it belongs to.</p>
     *
     * @param toolCallDelta       the fragment
     * @param completionId        the id shared by every chunk of this completion
     * @param model               the model that served the completion
     * @param createdEpochSeconds when the completion was created, in epoch seconds
     * @return the chunk
     */
    private static ObjectNode toolCallChunk(final InferenceStreamEvent.ToolCallDelta toolCallDelta,
                                            final String completionId,
                                            final String model,
                                            final long createdEpochSeconds) {

        final ObjectNode chunk = envelope(completionId, model, createdEpochSeconds);
        final ObjectNode choice = firstChoiceOf(chunk);
        final ObjectNode toolCall = choice.putObject("delta").putArray("tool_calls").addObject();

        final boolean firstFragment = toolCallDelta.id() != null || toolCallDelta.name() != null;

        toolCall.put("index", toolCallDelta.index());

        if (toolCallDelta.id() != null) {
            toolCall.put("id", toolCallDelta.id());
        }
        if (firstFragment) {
            toolCall.put("type", FUNCTION_TYPE);
        }

        final ObjectNode function = toolCall.putObject(FUNCTION_TYPE);

        if (toolCallDelta.name() != null) {
            function.put("name", toolCallDelta.name());
        }
        function.put("arguments", toolCallDelta.partialArguments());

        choice.putNull("finish_reason");

        return chunk;
    }

    /**
     * Builds the chunk that announces the end of generation.
     *
     * <p>Its delta is empty: the finish chunk says why the answer stopped, it does not add to the
     * answer. That distinction is what separates a completed answer from a truncated one.</p>
     *
     * @param finish              why generation stopped
     * @param completionId        the id shared by every chunk of this completion
     * @param model               the model that served the completion
     * @param createdEpochSeconds when the completion was created, in epoch seconds
     * @return the chunk
     */
    private static ObjectNode finishChunk(final InferenceStreamEvent.Finish finish,
                                          final String completionId,
                                          final String model,
                                          final long createdEpochSeconds) {

        final ObjectNode chunk = envelope(completionId, model, createdEpochSeconds);
        final ObjectNode choice = firstChoiceOf(chunk);

        choice.putObject("delta");
        choice.put("finish_reason", wireFinishReason(finish.reason()));

        return chunk;
    }

    /**
     * Builds the usage chunk.
     *
     * <p>Its {@code choices} array is present and empty, which is the shape standard clients
     * recognise as a usage-only chunk. Counts the provider did not report are left absent rather
     * than zeroed — a fabricated count is indistinguishable from a real one to anyone reconciling
     * spend.</p>
     *
     * @param usageEvent          the counts
     * @param completionId        the id shared by every chunk of this completion
     * @param model               the model that served the completion
     * @param createdEpochSeconds when the completion was created, in epoch seconds
     * @return the chunk
     */
    private static ObjectNode usageChunk(final InferenceStreamEvent.Usage usageEvent,
                                         final String completionId,
                                         final String model,
                                         final long createdEpochSeconds) {

        final ObjectNode chunk = envelope(completionId, model, createdEpochSeconds);
        chunk.putArray("choices");

        final InferenceUsage usage = usageEvent.usage();

        if (!usage.isReported()) {
            chunk.putNull("usage");
            return chunk;
        }

        final ObjectNode rendered = chunk.putObject("usage");

        if (usage.inputTokens() != null) {
            rendered.put("prompt_tokens", usage.inputTokens().intValue());
        }
        if (usage.outputTokens() != null) {
            rendered.put("completion_tokens", usage.outputTokens().intValue());
        }
        if (usage.totalTokens() != null) {
            rendered.put("total_tokens", usage.totalTokens().intValue());
        }

        return chunk;
    }

    /**
     * Builds the payload of a failed stream's last frame.
     *
     * <p>It is the standard top-level error object and nothing else — no chunk envelope, because
     * this frame is not a chunk of an answer. The conversion is
     * {@link InferenceErrorView#of(com.dotcms.inference.model.InferenceError)}, the one place that
     * owns the error's wire shape, so the streamed error stays byte-identical to the non-streamed
     * one. That conversion is also what keeps the HTTP status out of the body: retryability rides
     * on the response's status code, and the standard error shape has no field for it.</p>
     *
     * @param error the failure
     * @return the error payload
     */
    private static ObjectNode errorPayload(final InferenceStreamEvent.Error error) {

        return OBJECT_MAPPER.valueToTree(InferenceErrorView.of(error.error()));
    }

    /**
     * Starts a chunk with the envelope every chunk carries.
     *
     * @param completionId        the id shared by every chunk of this completion
     * @param model               the model that served the completion
     * @param createdEpochSeconds when the completion was created, in epoch seconds
     * @return the chunk, with no choices yet
     */
    private static ObjectNode envelope(final String completionId,
                                       final String model,
                                       final long createdEpochSeconds) {

        final ObjectNode chunk = OBJECT_MAPPER.createObjectNode();

        chunk.put("id", completionId);
        chunk.put("object", CHUNK_OBJECT);
        chunk.put("created", createdEpochSeconds);
        chunk.put("model", model);

        return chunk;
    }

    /**
     * Adds the single choice a content, tool-call or finish chunk carries.
     *
     * @param chunk the chunk being built
     * @return the choice at index 0
     */
    private static ObjectNode firstChoiceOf(final ObjectNode chunk) {

        final ObjectNode choice = chunk.putArray("choices").addObject();
        choice.put("index", 0);

        return choice;
    }

    /**
     * Maps an internal finish reason to its wire string.
     *
     * @param reason why generation stopped
     * @return the wire string a standard client expects
     * @throws IllegalArgumentException for {@link FinishReason#ERROR}, which has no wire
     *                                  equivalent: a failed stream ends with an error frame, not
     *                                  with a finish reason meaning "this failed"
     */
    private static String wireFinishReason(final FinishReason reason) {

        return switch (reason) {
            case STOP -> "stop";
            case LENGTH -> "length";
            case TOOL_CALLS -> "tool_calls";
            case CONTENT_FILTER -> "content_filter";
            case ERROR -> throw new IllegalArgumentException(
                    "FinishReason.ERROR has no wire equivalent; a failed stream ends with an "
                            + "error frame");
        };
    }
}
