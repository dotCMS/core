package com.dotcms.inference.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A completion request, in dotCMS's own terms.
 *
 * <p>This type is deliberately not a binding of any wire format. Parsing a client's payload into
 * it, and serializing a response back out, are the REST layer's job; nothing here mirrors the
 * structure of the JSON that arrives. The point is that tool-call identity, reasoning turns and
 * message ordering survive in a form a second wire format could also be written from, instead of
 * being flattened into whichever format happened to be implemented first.</p>
 *
 * <p>It is {@link Serializable} so it can travel as the payload of an
 * {@code AIRequest<InferenceRequest>} down the existing dotAI client pipe, which is what keeps
 * per-site provider caching and credential-rotation eviction working unchanged.</p>
 *
 * @param model                the model to use; required, with no implicit default
 * @param messages             the conversation, oldest first; at least one
 * @param tools                tools the model may call; possibly empty
 * @param toolChoice           the caller's tool preference, or null to leave it to the provider
 * @param responseFormat       the requested output shape, or null for the provider default
 * @param stream               whether to stream the answer
 * @param includeUsageInStream whether to emit a usage event on a stream; only meaningful when streaming
 * @param temperature          sampling temperature, or null to leave it to the provider
 * @param maxOutputTokens      ceiling on generated tokens, or null
 * @param topP                 nucleus sampling, or null
 * @param stopSequences        sequences that end generation; possibly empty
 */
public record InferenceRequest(String model,
                               List<InferenceMessage> messages,
                               List<InferenceToolSpec> tools,
                               ToolChoice toolChoice,
                               ResponseFormat responseFormat,
                               boolean stream,
                               boolean includeUsageInStream,
                               Double temperature,
                               Integer maxOutputTokens,
                               Double topP,
                               List<String> stopSequences) implements Serializable {

    public InferenceRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("InferenceRequest model is required");
        }
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("InferenceRequest requires at least one message");
        }
        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
        stopSequences = stopSequences == null ? List.of() : List.copyOf(stopSequences);
        requireToolResultsAreCorrelated(messages);
    }

    /**
     * Rejects a tool result that answers a call nobody made.
     *
     * <p>A provider given an orphaned tool turn fails in its own way, at its own layer, having
     * already been paid for the round trip. Catching it here turns that into a validation error
     * naming the offending identity.</p>
     *
     * @param messages the conversation to check
     */
    private static void requireToolResultsAreCorrelated(final List<InferenceMessage> messages) {
        final Set<String> offered = new HashSet<>();
        for (final InferenceMessage message : messages) {
            if (message.role() == Role.TOOL && !offered.contains(message.toolCallId())) {
                throw new IllegalArgumentException(
                        "Tool result references '" + message.toolCallId()
                                + "', which no preceding assistant message requested");
            }
            message.toolCalls().forEach(call -> offered.add(call.id()));
        }
    }

    /** @return whether the caller declared any tools */
    public boolean hasTools() {
        return !tools.isEmpty();
    }

    /**
     * @param model the model to use
     * @return a builder seeded with the required model
     */
    public static Builder builder(final String model) {
        return new Builder(model);
    }

    /** Assembles an {@link InferenceRequest}; validation happens on {@link #build()}. */
    public static final class Builder {

        private final String model;
        private List<InferenceMessage> messages = List.of();
        private List<InferenceToolSpec> tools = List.of();
        private ToolChoice toolChoice;
        private ResponseFormat responseFormat;
        private boolean stream;
        private boolean includeUsageInStream;
        private Double temperature;
        private Integer maxOutputTokens;
        private Double topP;
        private List<String> stopSequences = List.of();

        private Builder(final String model) {
            this.model = model;
        }

        public Builder messages(final List<InferenceMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder tools(final List<InferenceToolSpec> tools) {
            this.tools = tools;
            return this;
        }

        public Builder toolChoice(final ToolChoice toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder responseFormat(final ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder stream(final boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder includeUsageInStream(final boolean includeUsageInStream) {
            this.includeUsageInStream = includeUsageInStream;
            return this;
        }

        public Builder temperature(final Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxOutputTokens(final Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
            return this;
        }

        public Builder topP(final Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder stopSequences(final List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public InferenceRequest build() {
            return new InferenceRequest(model, messages, tools, toolChoice, responseFormat, stream,
                    includeUsageInStream, temperature, maxOutputTokens, topP, stopSequences);
        }
    }
}
