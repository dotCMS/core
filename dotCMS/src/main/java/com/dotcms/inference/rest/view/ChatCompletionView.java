package com.dotcms.inference.rest.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * A completed answer, in the wire shape clients deserialize.
 *
 * <p>Not wrapped in the dotCMS {@code ResponseEntityView} envelope. Byte compatibility with the
 * external standard is the whole point of this endpoint family, and a wrapper would stop the
 * payload deserializing into the result type a client library already has.</p>
 *
 * @param id      identity for this completion
 * @param object  always {@code chat.completion}
 * @param created creation time, epoch seconds
 * @param model   the model that actually served the request, after any fallback
 * @param choices the answers; always exactly one, as several are not supported
 * @param usage   tokens consumed, omitted when the provider did not report them
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Chat completion response in the OpenAI-compatible shape")
public record ChatCompletionView(
        @JsonProperty("id") @Schema(description = "Completion id", example = "chatcmpl-8f3a") String id,
        @JsonProperty("object") @Schema(description = "Object type", example = "chat.completion") String object,
        @JsonProperty("created") @Schema(description = "Epoch seconds") long created,
        @JsonProperty("model") @Schema(description = "Model that served the request", example = "gpt-4o") String model,
        @JsonProperty("choices") @Schema(description = "The answers") List<ChoiceView> choices,
        @JsonProperty("usage") @Schema(description = "Tokens consumed") UsageView usage) {

    /** Object type for a completed answer. */
    public static final String OBJECT = "chat.completion";

    /**
     * One answer.
     *
     * @param index        position; always 0
     * @param message      the assistant turn
     * @param finishReason why generation stopped
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChoiceView(
            @JsonProperty("index") int index,
            @JsonProperty("message") MessageView message,
            @JsonProperty("finish_reason") String finishReason) {
    }

    /**
     * The assistant turn, carrying content or tool calls.
     *
     * @param role      always {@code assistant}
     * @param content   the text, null when the model asked for tools instead
     * @param toolCalls the tools the model wants executed
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageView(
            @JsonProperty("role") String role,
            @JsonProperty("content") String content,
            @JsonProperty("tool_calls") List<ToolCallView> toolCalls) {
    }

    /**
     * A tool the model is asking to run.
     *
     * @param id       identity the client echoes back on the tool result
     * @param type     always {@code function}
     * @param function the call itself
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolCallView(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("function") FunctionCallView function) {
    }

    /**
     * @param name      the tool to run
     * @param arguments its arguments as JSON text
     */
    public record FunctionCallView(
            @JsonProperty("name") String name,
            @JsonProperty("arguments") String arguments) {
    }

    /**
     * @param promptTokens     tokens in the prompt
     * @param completionTokens tokens generated
     * @param totalTokens      the sum
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UsageView(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }
}
