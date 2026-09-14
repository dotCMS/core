package com.dotcms.inference.rest.mapper;

import com.dotcms.inference.model.FinishReason;
import com.dotcms.inference.model.InferenceMessage;
import com.dotcms.inference.model.InferenceRequest;
import com.dotcms.inference.model.InferenceResponse;
import com.dotcms.inference.model.InferenceToolCall;
import com.dotcms.inference.model.InferenceToolSpec;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.model.ResponseFormat;
import com.dotcms.inference.model.Role;
import com.dotcms.inference.model.ToolChoice;
import com.dotcms.inference.rest.view.ChatCompletionRequestView;
import com.dotcms.inference.rest.view.ChatCompletionView;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Translates between the OpenAI-compatible chat-completions wire shape and dotCMS's
 * provider-neutral inference model.
 *
 * <p>The wire shape and {@link InferenceRequest} are kept deliberately independent of each other,
 * so this class is the single place where the two vocabularies meet. Everything that identifies a
 * turn — a tool call's provider-assigned id, the unparsed JSON text of its arguments, the JSON
 * Schema of a declared tool — is carried across verbatim: re-deriving any of it (from position,
 * say) would bake one serialization into the model and break the moment a second wire format is
 * written from the same internal request.</p>
 *
 * <p>Validation policy is asymmetric on purpose. A field this family has no opinion about is
 * ignored, because rejecting a client's default payload over a field that changes nothing is
 * pure friction. A field that changes what the caller gets or pays for — {@code n} above all — is
 * rejected loudly instead, since a request quietly served as something other than what was asked
 * is worse than one that failed. Every validation error names the offending field so the caller
 * is not sent back to guessing.</p>
 *
 * <p>Stateless and thread-safe; it holds nothing.</p>
 */
public final class ChatCompletionMapper {

    private static final String FUNCTION_TYPE = "function";

    /** Static utility; never instantiated. */
    private ChatCompletionMapper() {
        throw new AssertionError("ChatCompletionMapper is a static utility");
    }

    /**
     * Binds an inbound chat-completions payload to the internal request the rest of the pipeline
     * speaks.
     *
     * <p>Wire-level validation happens here rather than deeper in, so a malformed request fails
     * before any provider is chosen and before anybody is billed for a round trip. Correlation of
     * tool results against the calls that requested them is deliberately <em>not</em> repeated:
     * {@link InferenceRequest}'s compact constructor already enforces it, and duplicating the rule
     * would mean two places to keep in step.</p>
     *
     * @param view the bound wire payload
     * @return the equivalent internal request
     * @throws IllegalArgumentException when the payload is missing a required field or asks for
     *                                  something this family cannot serve, the message naming the
     *                                  offending field
     */
    public static InferenceRequest toInferenceRequest(final ChatCompletionRequestView view) {

        if (view == null) {
            throw new IllegalArgumentException("A chat completion request body is required");
        }
        if (view.model() == null || view.model().isBlank()) {
            throw new IllegalArgumentException(
                    "The model field is required; there is no implicit default model");
        }
        if (view.messages() == null || view.messages().isEmpty()) {
            throw new IllegalArgumentException(
                    "The messages field requires at least one message to infer from");
        }
        if (view.n() != null && view.n() > 1) {
            throw new IllegalArgumentException(
                    "The field n is not supported; exactly one choice is returned, so n was "
                            + "rejected rather than silently ignored");
        }

        return InferenceRequest.builder(view.model())
                .messages(toInferenceMessages(view.messages()))
                .tools(toToolSpecs(view.tools()))
                .toolChoice(toToolChoice(view.toolChoice()))
                .responseFormat(toResponseFormat(view.responseFormat()))
                .stream(Boolean.TRUE.equals(view.stream()))
                .includeUsageInStream(view.streamOptions() != null
                        && Boolean.TRUE.equals(view.streamOptions().includeUsage()))
                .temperature(view.temperature())
                .maxOutputTokens(view.maxTokens())
                .topP(view.topP())
                .stopSequences(view.stop())
                .build();
    }

    /**
     * Renders a completed answer in the wire shape standard clients deserialize.
     *
     * <p>Two details here are the point of the method. The model reported is the one that actually
     * served the request, not the one asked for, which is the only signal a caller has that a
     * site's fallback chain moved on. And usage the provider never reported is left absent rather
     * than rendered as zeros — a fabricated count is indistinguishable from a real one to anyone
     * reconciling spend.</p>
     *
     * @param response the completed internal response
     * @return the wire view of that response
     * @throws IllegalArgumentException when there is no response to render
     */
    public static ChatCompletionView toView(final InferenceResponse response) {

        if (response == null) {
            throw new IllegalArgumentException("An inference response is required to render a view");
        }

        final InferenceMessage message = response.message();
        final ChatCompletionView.MessageView messageView = new ChatCompletionView.MessageView(
                wireRole(message.role()),
                message.content(),
                toToolCallViews(message.toolCalls()));

        final ChatCompletionView.ChoiceView choice = new ChatCompletionView.ChoiceView(
                0, messageView, wireFinishReason(response.finishReason()));

        return new ChatCompletionView(
                response.id(),
                ChatCompletionView.OBJECT,
                response.createdEpochSeconds(),
                response.model(),
                List.of(choice),
                toUsageView(response.usage()));
    }

    // ---------------------------------------------------------------------
    // Inbound
    // ---------------------------------------------------------------------

    /**
     * Maps the conversation, preserving turn order.
     *
     * @param messageViews the wire turns, oldest first
     * @return the internal turns, in the same order
     */
    private static List<InferenceMessage> toInferenceMessages(
            final List<ChatCompletionRequestView.MessageView> messageViews) {

        final List<InferenceMessage> messages = new ArrayList<>(messageViews.size());
        for (final ChatCompletionRequestView.MessageView messageView : messageViews) {
            messages.add(new InferenceMessage(
                    toRole(messageView.role()),
                    messageView.content(),
                    toToolCalls(messageView.toolCalls()),
                    messageView.toolCallId(),
                    messageView.name()));
        }

        return messages;
    }

    /**
     * @param role the wire role name
     * @return the matching constant
     * @throws IllegalArgumentException when the role is missing or is not one this format defines
     */
    private static Role toRole(final String role) {

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Each message requires a role");
        }

        switch (role.trim().toLowerCase(Locale.ROOT)) {
            case "system":
                return Role.SYSTEM;
            case "user":
                return Role.USER;
            case "assistant":
                return Role.ASSISTANT;
            case "tool":
                return Role.TOOL;
            default:
                throw new IllegalArgumentException("Unsupported message role '" + role + "'");
        }
    }

    /**
     * Maps replayed tool calls, keeping the provider-assigned id and the arguments exactly as the
     * model produced them. The index is the position within the turn, retained only so a
     * serializer can reproduce it; identity is never derived from it.
     *
     * @param toolCallViews the wire tool calls, possibly null
     * @return the internal tool calls; empty when none were sent
     */
    private static List<InferenceToolCall> toToolCalls(
            final List<ChatCompletionRequestView.ToolCallView> toolCallViews) {

        if (toolCallViews == null || toolCallViews.isEmpty()) {
            return List.of();
        }

        final List<InferenceToolCall> toolCalls = new ArrayList<>(toolCallViews.size());
        for (int index = 0; index < toolCallViews.size(); index++) {
            final ChatCompletionRequestView.ToolCallView toolCallView = toolCallViews.get(index);
            final ChatCompletionRequestView.FunctionCallView function = toolCallView.function();
            if (function == null) {
                throw new IllegalArgumentException(
                        "Tool call '" + toolCallView.id() + "' carries no function to call");
            }
            toolCalls.add(new InferenceToolCall(
                    toolCallView.id(), function.name(), function.arguments(), index));
        }

        return toolCalls;
    }

    /**
     * Maps tool declarations, passing the JSON Schema through as a tree — dotCMS neither
     * interprets nor validates it, it is the caller's contract with their own tool.
     *
     * @param toolViews the declared tools, possibly null
     * @return the internal specs; empty when none were declared
     */
    private static List<InferenceToolSpec> toToolSpecs(
            final List<ChatCompletionRequestView.ToolView> toolViews) {

        if (toolViews == null || toolViews.isEmpty()) {
            return List.of();
        }

        final List<InferenceToolSpec> specs = new ArrayList<>(toolViews.size());
        for (final ChatCompletionRequestView.ToolView toolView : toolViews) {
            final ChatCompletionRequestView.FunctionDefView function = toolView.function();
            if (function == null) {
                throw new IllegalArgumentException("Each entry in tools requires a function");
            }
            specs.add(new InferenceToolSpec(
                    function.name(), function.description(), function.parameters()));
        }

        return specs;
    }

    /**
     * Reads the {@code tool_choice} field, which the format spells either as one of the strings
     * {@code auto} / {@code required} / {@code none} or as an object naming one function.
     *
     * @param node the raw field, possibly null
     * @return the caller's preference, or null to leave the decision to the provider
     * @throws IllegalArgumentException when the field is present but is neither of those shapes
     */
    private static ToolChoice toToolChoice(final JsonNode node) {

        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        if (node.isTextual()) {
            switch (node.asText().trim().toLowerCase(Locale.ROOT)) {
                case "auto":
                    return ToolChoice.auto();
                case "required":
                    return new ToolChoice(ToolChoice.Mode.REQUIRED, null);
                case "none":
                    return new ToolChoice(ToolChoice.Mode.NONE, null);
                default:
                    throw new IllegalArgumentException(
                            "Unsupported tool_choice '" + node.asText() + "'");
            }
        }

        if (node.isObject()) {
            final JsonNode name = node.path(FUNCTION_TYPE).path("name");
            if (!name.isTextual() || name.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "An object tool_choice must name the function to call");
            }
            return ToolChoice.function(name.asText());
        }

        throw new IllegalArgumentException(
                "tool_choice must be a string or an object naming a function");
    }

    /**
     * Reads the {@code response_format} field.
     *
     * @param node the raw field, possibly null
     * @return the requested output shape, or null for the provider default
     * @throws IllegalArgumentException when the field is present but names no supported type
     */
    private static ResponseFormat toResponseFormat(final JsonNode node) {

        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        final JsonNode type = node.path("type");
        if (!type.isTextual()) {
            throw new IllegalArgumentException("response_format must name a type");
        }

        switch (type.asText().trim().toLowerCase(Locale.ROOT)) {
            case "text":
                return ResponseFormat.text();
            case "json_object":
                return new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null);
            case "json_schema":
                final JsonNode schema = node.path("json_schema").path("schema");
                if (schema.isMissingNode() || schema.isNull()) {
                    throw new IllegalArgumentException(
                            "A json_schema response_format requires json_schema.schema");
                }
                return new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, schema);
            default:
                throw new IllegalArgumentException(
                        "Unsupported response_format type '" + type.asText() + "'");
        }
    }

    // ---------------------------------------------------------------------
    // Outbound
    // ---------------------------------------------------------------------

    /**
     * @param role the internal author of the turn
     * @return the wire spelling of that role
     */
    private static String wireRole(final Role role) {
        return role.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Renders why generation stopped.
     *
     * <p>{@link FinishReason#ERROR} has no equivalent in this format — a failed exchange is an
     * error event, not a finish reason — so it renders as absent rather than as an invented
     * reason a client would read as a successful stop.</p>
     *
     * @param finishReason the internal reason
     * @return the wire string, or null when the reason has no wire equivalent
     */
    private static String wireFinishReason(final FinishReason finishReason) {

        switch (finishReason) {
            case STOP:
                return "stop";
            case LENGTH:
                return "length";
            case TOOL_CALLS:
                return "tool_calls";
            case CONTENT_FILTER:
                return "content_filter";
            default:
                return null;
        }
    }

    /**
     * @param toolCalls the calls the model is asking for
     * @return the wire tool calls, or null when there are none, so the field is omitted entirely
     */
    private static List<ChatCompletionView.ToolCallView> toToolCallViews(
            final List<InferenceToolCall> toolCalls) {

        if (toolCalls == null || toolCalls.isEmpty()) {
            return null;
        }

        final List<ChatCompletionView.ToolCallView> views = new ArrayList<>(toolCalls.size());
        for (final InferenceToolCall toolCall : toolCalls) {
            views.add(new ChatCompletionView.ToolCallView(
                    toolCall.id(),
                    FUNCTION_TYPE,
                    new ChatCompletionView.FunctionCallView(
                            toolCall.name(), toolCall.arguments())));
        }

        return views;
    }

    /**
     * @param usage the internal counts
     * @return the wire usage, or null when the provider reported nothing — absent counts are never
     *         rendered as zeros
     */
    private static ChatCompletionView.UsageView toUsageView(final InferenceUsage usage) {

        if (usage == null || !usage.isReported()) {
            return null;
        }

        return new ChatCompletionView.UsageView(
                usage.inputTokens(), usage.outputTokens(), usage.totalTokens());
    }
}
