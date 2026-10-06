package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The inbound chat-completion request, in the wire shape clients send.
 *
 * <p>Unknown properties are ignored so a client's default payload does not fail on a field this
 * family has no opinion about. That tolerance is deliberate, not laxness: the format gains fields
 * steadily and clients send {@code user}, {@code store} and {@code service_tier} unprompted, so
 * binding strictly would break a working caller on its next upgrade.</p>
 *
 * <p>The cost is that a dropped field is indistinguishable from one never sent, and three of
 * them do change what a provider returns: {@code frequency_penalty}, {@code presence_penalty}
 * and {@code seed}. None is modelled here, so a request carrying them succeeds while they do
 * nothing — a caller setting {@code seed} for reproducible output gets varying output back with
 * no indication why. They are accepted anyway, because refusing a field a client may send by
 * default breaks that client for no gain, and because only the four sampling parameters below
 * reach a provider at all; honouring the others would mean carrying them through the request
 * model and every provider strategy, which is a larger decision than this record. Should that
 * change, {@code seed} is the one worth carrying first.</p>
 *
 * <p>{@code n} is the exception that is refused rather than ignored: silently returning one
 * choice to a caller who asked for four is a wrong answer, not a missing refinement.</p>
 *
 * @param model            the model to use; required, no implicit default
 * @param messages         the conversation, oldest first
 * @param tools            tools the model may call
 * @param toolChoice       the caller's tool preference
 * @param responseFormat   the requested output shape
 * @param stream           whether to stream
 * @param streamOptions    streaming options, notably whether to include usage
 * @param temperature      sampling temperature, passed through
 * @param maxTokens        ceiling on generated tokens, passed through
 * @param topP             nucleus sampling, passed through
 * @param stop             stop sequences, passed through
 * @param n                number of choices; unsupported and rejected rather than ignored
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Chat completion request in the OpenAI-compatible shape")
public record ChatCompletionRequestView(
        @JsonProperty("model") @Schema(description = "Model id, as listed by GET /models", example = "gpt-4o") String model,
        @JsonProperty("messages") @Schema(description = "The conversation, oldest first") List<MessageView> messages,
        @JsonProperty("tools") @Schema(description = "Tools the model may call") List<ToolView> tools,
        @JsonProperty("tool_choice") @Schema(description = "Tool selection preference") JsonNode toolChoice,
        @JsonProperty("response_format") @Schema(description = "Requested output format") JsonNode responseFormat,
        @JsonProperty("stream") @Schema(description = "Stream the answer as server-sent events") Boolean stream,
        @JsonProperty("stream_options") @Schema(description = "Streaming options") StreamOptionsView streamOptions,
        @JsonProperty("temperature") @Schema(description = "Sampling temperature") Double temperature,
        @JsonProperty("max_tokens") @Schema(description = "Maximum tokens to generate") Integer maxTokens,
        @JsonProperty("top_p") @Schema(description = "Nucleus sampling") Double topP,
        @JsonProperty("stop") @Schema(description = "Stop sequences") List<String> stop,
        @JsonProperty("n") @Schema(description = "Number of choices; not supported") Integer n) {

    /**
     * One turn in the conversation.
     *
     * <p>{@code content} is a {@link JsonNode} rather than a String because the format allows two
     * shapes for it: a plain string, and an array of typed parts. Clients built on the OpenAI
     * libraries send the string form for an ordinary text turn and switch to the array form as
     * soon as a turn carries anything else — an image, a file — or more than one part. Binding
     * the field to String accepted the first and failed the second inside Jackson, which produces
     * a deserialization error naming a Java type instead of a refusal the caller can act on. The
     * array is flattened, or refused by name, in the mapper.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageView(
            @JsonProperty("role") String role,
            @JsonProperty("content") @Schema(description =
                    "The turn's content: a string, or an array of text parts") JsonNode content,
            @JsonProperty("tool_calls") List<ToolCallView> toolCalls,
            @JsonProperty("tool_call_id") String toolCallId,
            @JsonProperty("name") String name) {

        /**
         * Convenience for the string form, which is what most turns carry.
         *
         * <p>Calling this with a null content is ambiguous against the canonical constructor and
         * will not compile; cast the null to pick one. That is deliberate — an assistant turn
         * that is only tool calls has null content and should say so explicitly.</p>
         *
         * @param role       the turn's role
         * @param content    the turn's text
         * @param toolCalls  tool calls replayed by the client, or null
         * @param toolCallId the call a tool turn answers, or null
         * @param name       the tool's name, or null
         */
        public MessageView(final String role,
                           final String content,
                           final List<ToolCallView> toolCalls,
                           final String toolCallId,
                           final String name) {
            this(role, content == null ? null : TextNode.valueOf(content), toolCalls, toolCallId,
                    name);
        }
    }

    /** A tool call the model previously asked for, replayed by the client. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCallView(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("function") FunctionCallView function) {
    }

    /** The function a tool call names, with its arguments as JSON text. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCallView(
            @JsonProperty("name") String name,
            @JsonProperty("arguments") String arguments) {
    }

    /** A tool declaration. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolView(
            @JsonProperty("type") String type,
            @JsonProperty("function") FunctionDefView function) {
    }

    /** A declared function's name, description and JSON Schema parameters. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionDefView(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("parameters") JsonNode parameters) {
    }

    /** Streaming options; {@code include_usage} is what gates the usage event. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StreamOptionsView(@JsonProperty("include_usage") Boolean includeUsage) {
    }
}
