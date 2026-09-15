package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * The inbound chat-completion request, in the wire shape clients send.
 *
 * <p>Unknown properties are ignored so a client's default payload does not fail on a field this
 * family has no opinion about. That tolerance stops at fields which change what the caller gets
 * or pays for — those are rejected by the mapper rather than silently dropped, because a request
 * that quietly did something other than what was asked is worse than one that failed.</p>
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

    /** One turn in the conversation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MessageView(
            @JsonProperty("role") String role,
            @JsonProperty("content") String content,
            @JsonProperty("tool_calls") List<ToolCallView> toolCalls,
            @JsonProperty("tool_call_id") String toolCallId,
            @JsonProperty("name") String name) {
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
