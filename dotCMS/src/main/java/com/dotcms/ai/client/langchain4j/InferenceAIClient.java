package com.dotcms.ai.client.langchain4j;

import com.dotcms.ai.app.AppConfig;
import com.dotcms.inference.model.CallerSafeException;
import com.dotcms.inference.model.InferenceError;
import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.model.MultipleImagesUnsupportedException;
import com.dotcms.inference.model.InferenceMessage;
import com.dotcms.inference.model.InferenceRequest;
import com.dotcms.inference.model.InferenceResponse;
import com.dotcms.inference.model.InferenceStreamEvent;
import com.dotcms.inference.model.InferenceToolCall;
import com.dotcms.inference.model.InferenceToolSpec;
import com.dotcms.inference.model.InferenceUsage;
import com.dotcms.inference.model.Role;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.vavr.Lazy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Drives completions for the {@code /api/inference/v1} family against LangChain4J.
 *
 * <p>dotCMS has two AI surfaces and they are under different pressures. {@code /api/v1/ai/*}
 * evolves when dotCMS decides it should. This family is pinned to an external standard that
 * nobody here controls, so it changes when that standard changes — on someone else's schedule.
 * Keeping its request and response semantics in a class of their own is what lets that happen
 * without editing a line that a shipped endpoint depends on.</p>
 *
 * <p>What deliberately did <strong>not</strong> move here is model construction, caching and the
 * per-site fallback chain. Those stay in {@link LangChain4jAIClient} because
 * {@code LangChain4jAIClient.flushCachesForHost(String)} is what evicts a site's cached providers
 * when its credentials are rotated, and {@code AIAppListener} is wired to that one class. A second
 * client keeping its own cache would go on serving a revoked key until its TTL expired, with
 * nothing to notice. So this class borrows models through
 * {@link LangChain4jAIClient#withChatModel}, {@link LangChain4jAIClient#withStreamingChatModel},
 * {@link LangChain4jAIClient#withEmbeddingModel} and {@link LangChain4jAIClient#withImageModel},
 * and owns no state of its own.</p>
 *
 * <p>Those accessors hand over the name of the model that actually served, which after a fallback
 * hop is not the one that was asked for. That name — not {@link InferenceRequest#model()} — is
 * what {@link InferenceResponse#model()} reports, so a caller can tell a fallback happened. For
 * the same reason the requested model is never set on the outgoing {@link ChatRequest}: the site's
 * configuration, not the caller, decides which provider model runs.</p>
 *
 * <p>Nothing here logs message content, tool arguments or provider error envelopes: they carry
 * customer data, and an error event is given a description written here rather than whatever the
 * provider echoed back.</p>
 */
public final class InferenceAIClient {

    private static final Lazy<InferenceAIClient> INSTANCE = Lazy.of(InferenceAIClient::new);

    private static final String RESPONSE_ID_PREFIX = "chatcmpl-";
    private static final String RESPONSE_SCHEMA_NAME = "response";
    private static final String UPSTREAM_FAILURE_MESSAGE =
            "The model provider failed to complete the request";
    private static final String SCHEMA_PROPERTIES = "properties";
    private static final String SCHEMA_REQUIRED = "required";
    private static final String SCHEMA_DESCRIPTION = "description";
    private static final String SCHEMA_ADDITIONAL_PROPERTIES = "additionalProperties";
    private static final String SCHEMA_DEFS = "$defs";
    private static final String SCHEMA_DEFINITIONS = "definitions";

    /** What a caller is told when nothing usable came back from an image provider. */
    private static final String NO_IMAGE_MESSAGE = "The model provider returned no usable image";

    /**
     * Ceiling on fetching a provider-hosted image. A request thread is parked for the whole
     * download, so a provider whose CDN hangs must not be able to hold one indefinitely.
     */
    private static final int IMAGE_FETCH_TIMEOUT_SECONDS = 30;

    private InferenceAIClient() {
    }

    /**
     * @return the single instance; mirrors {@link LangChain4jAIClient#get()} because this class,
     *         like that one, holds no per-request state
     */
    public static InferenceAIClient get() {
        return INSTANCE.get();
    }

    /**
     * Runs one completion and waits for the whole answer.
     *
     * <p>The model is borrowed from {@link LangChain4jAIClient}, so a site's fallback chain applies
     * and the returned {@link InferenceResponse#model()} names the model that actually served.
     * Usage is reported only when the provider reported it; when it did not, the response carries
     * {@link InferenceUsage#UNREPORTED} rather than counts invented here.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param request   the completion to run
     * @return the assistant's turn, which may ask for tools instead of carrying text
     * @throws RuntimeException if every model in the site's chain failed; the exception is the last
     *                          failure, left for the REST layer to turn into a status
     */
    public InferenceResponse complete(final AppConfig appConfig, final InferenceRequest request) {
        final ChatRequest chatRequest = toChatRequest(request);
        return LangChain4jAIClient.get().withChatModel(
                appConfig,
                (model, servingModel) -> toInferenceResponse(model.chat(chatRequest), servingModel));
    }

    /**
     * Runs one completion and hands each event to {@code sink} as it arrives.
     *
     * <p>The sink sees content fragments, tool-call fragments, and exactly one terminal event: a
     * {@link InferenceStreamEvent.Finish} when generation ended, or an
     * {@link InferenceStreamEvent.Error} when it did not. Never both — after the first event is
     * written the HTTP status is already gone, so which variant ends the stream is the only way a
     * caller can tell a failed answer from a complete one. A
     * {@link InferenceStreamEvent.Usage} event follows the finish only when the caller set
     * {@link InferenceRequest#includeUsageInStream()}; sending it unasked breaks readers that
     * reject the empty choices array it serializes to.</p>
     *
     * <p>{@link InferenceLimits#completionTimeoutSeconds()} is a hard ceiling: a provider that
     * stops producing events ends the stream with an error rather than parking the calling thread
     * indefinitely. Failures are reported through the sink rather than thrown, because once
     * streaming has begun the sink is the only channel left.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param request   the completion to run
     * @param sink      receives every event, in order
     */
    public void stream(final AppConfig appConfig,
                       final InferenceRequest request,
                       final Consumer<InferenceStreamEvent> sink) {
        final ChatRequest chatRequest = toChatRequest(request);
        final StreamState state = new StreamState(sink, request.includeUsageInStream());
        try {
            LangChain4jAIClient.get().withStreamingChatModel(
                    appConfig,
                    (model, servingModel) -> streamWithModel(model, chatRequest, state));
        } catch (final RuntimeException e) {
            // Nothing reached the sink — every model in the chain failed to start — so the sink
            // still has to learn that this stream will not finish.
            // Logged with the exception rather than just its type. What the caller receives is a
            // safe sentence that says the detail is in the log, so the detail has to actually be
            // here — a bare class name leaves an operator with "InvalidRequestException" and no
            // way to tell an exhausted account from a rejected key from a malformed request. The
            // buffered path has always logged the full exception; this matches it.
            Logger.warn(InferenceAIClient.class, "Inference stream could not be started", e);
            state.fail(toInferenceError(e));
        }
    }

    /**
     * Embeds a batch of texts in one provider round trip.
     *
     * <p>One call rather than one per input, because the batch is what the caller sent and what
     * the provider bills as a unit: embedding them separately would report usage that describes
     * none of them and would multiply the site's spend on a request it was never asked to split.
     * The vectors come back in the order the inputs were given, which is what lets the caller
     * stamp each entry with the index of the text it embedded.</p>
     *
     * <p>Nothing here logs the input: it is customer content by definition.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param inputs    the texts to embed, in the order the caller sent them
     * @return the vectors, the model that served, and the usage for the whole batch
     * @throws RuntimeException if every model in the site's chain failed; the exception is the last
     *                          failure, left for the REST layer to turn into a status
     */
    public EmbeddingBatch embed(final AppConfig appConfig, final List<String> inputs) {
        final List<TextSegment> segments = new ArrayList<>(inputs.size());
        for (final String input : inputs) {
            segments.add(TextSegment.from(input));
        }

        return LangChain4jAIClient.get().withEmbeddingModel(appConfig, (model, servingModel) -> {
            final Response<List<Embedding>> response = model.embedAll(segments);
            final List<Embedding> embeddings =
                    response.content() == null ? List.of() : response.content();
            final List<List<Float>> vectors = new ArrayList<>(embeddings.size());
            for (final Embedding embedding : embeddings) {
                vectors.add(List.copyOf(embedding.vectorAsList()));
            }
            return new EmbeddingBatch(
                    servingModel, List.copyOf(vectors), toEmbeddingUsage(response.tokenUsage()));
        });
    }

    /**
     * Generates images from a prompt and returns each as base64, whatever the provider offered.
     *
     * <p>{@link LangChain4jAIClient#withImageModel} already asks the provider for the inline form,
     * so for most providers the bytes arrive inline and nothing further is needed. A provider that
     * has no such option answers with a URL regardless; that URL is fetched and re-encoded here
     * rather than refused, because the upstream artifact exists either way at that point and
     * declining would break image generation on part of a multi-provider gateway to avoid an
     * exposure already incurred. What the guarantee actually covers is the caller: they never
     * receive an addressable artifact.</p>
     *
     * <p>A request for a single image goes through {@code generate(prompt)} rather than through
     * {@code generate(prompt, 1)}. The multi-image overload is a {@code default} method on
     * {@link ImageModel} that throws unless a provider overrides it, so routing the common case
     * through it would break every provider that has not — for a count where the two calls mean
     * exactly the same thing.</p>
     *
     * <p>Neither the prompt nor the provider's URL is logged; the first is customer content and
     * the second addresses content generated from it.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param prompt    what to generate
     * @param size      the size the caller asked for, or null to use the site's configured one
     * @param count     how many images to generate; at least one
     * @return the images as base64, and the model that served
     * @throws RuntimeException if every model in the site's chain failed, if the provider cannot
     *                          generate several images at once, or if it produced nothing that
     *                          could be turned into bytes
     */
    public GeneratedImages generateImages(final AppConfig appConfig,
                                          final String prompt,
                                          final String size,
                                          final int count) {
        return LangChain4jAIClient.get().withImageModel(appConfig, size, (model, servingModel) -> {
            if (count > 1 && !supportsMultipleImages(model)) {
                throw new MultipleImagesUnsupportedException(servingModel);
            }
            final List<Image> images = count == 1
                    ? Collections.singletonList(model.generate(prompt).content())
                    : model.generate(prompt, count).content();

            if (images == null || images.isEmpty()) {
                throw new IllegalStateException(NO_IMAGE_MESSAGE);
            }

            final List<GeneratedImage> generated = new ArrayList<>(images.size());
            for (final Image image : images) {
                generated.add(new GeneratedImage(
                        toBase64(image), image == null ? null : image.revisedPrompt()));
            }
            return new GeneratedImages(servingModel, List.copyOf(generated));
        });
    }

    /**
     * Answers whether an image model can actually produce more than one image per call.
     *
     * <p>The multi-image call is a default method on the provider abstraction that throws unless
     * the implementation overrides it, so declaring the image capability says nothing about
     * whether this particular provider honours a count. Asking the class which one it inherited is
     * exact: it needs no list of provider names to be kept current, and it cannot be fooled by a
     * message string that the library is free to change.</p>
     *
     * <p>Probing beforehand rather than catching afterwards matters because the thrown type is
     * {@code IllegalArgumentException} — indistinguishable from a genuine complaint about the
     * arguments, and so not safe to translate on sight.</p>
     *
     * @param model the image model handed over by the accessor
     * @return whether the multi-image call is implemented rather than inherited
     */
    private static boolean supportsMultipleImages(final ImageModel model) {
        try {
            return model.getClass()
                    .getMethod("generate", String.class, int.class)
                    .getDeclaringClass() != ImageModel.class;
        } catch (final NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Drives one streaming exchange to its terminal event.
     *
     * <p>Nothing is thrown out of here once an event has been emitted: the fallback chain in
     * {@link LangChain4jAIClient} retries on a thrown exception, and a retry after the sink has
     * seen part of an answer would splice two streams together. Before the first event there is
     * nothing to splice, so a failure there is rethrown and the next model gets its turn.</p>
     *
     * @param model       the streaming model handed over by the accessor
     * @param chatRequest the mapped request
     * @param state       collects events and guarantees a single terminal one
     */
    private void streamWithModel(final StreamingChatModel model,
                                 final ChatRequest chatRequest,
                                 final StreamState state) {
        final int timeoutSeconds = InferenceLimits.current().completionTimeoutSeconds();
        try {
            model.chat(chatRequest, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(final String token) {
                    state.contentDelta(token);
                }

                @Override
                public void onPartialToolCall(final PartialToolCall partialToolCall) {
                    state.partialToolCall(partialToolCall);
                }

                @Override
                public void onCompleteToolCall(final CompleteToolCall completeToolCall) {
                    state.completeToolCall(completeToolCall);
                }

                @Override
                public void onCompleteResponse(final ChatResponse response) {
                    state.finish(response);
                }

                @Override
                public void onError(final Throwable throwable) {
                    Logger.warn(InferenceAIClient.class, "Inference stream failed", throwable);
                    state.fail(toInferenceError(throwable));
                }
            });
        } catch (final RuntimeException e) {
            if (!state.hasEmitted()) {
                throw e;
            }
            Logger.warn(InferenceAIClient.class,
                    "Inference stream aborted: " + e.getClass().getSimpleName());
            state.fail(toInferenceError(e));
            return;
        }

        if (!state.awaitTerminal(timeoutSeconds)) {
            Logger.warn(InferenceAIClient.class,
                    "Inference stream exceeded the " + timeoutSeconds + " second ceiling");
            state.fail(InferenceError.upstream(
                    "The completion exceeded the configured ceiling of " + timeoutSeconds + " seconds"));
        }
    }

    /**
     * Maps a completion request into the provider-facing request.
     *
     * <p>The requested model is not mapped: the site's configuration chooses which provider model
     * runs, and overriding it here would defeat the fallback chain the accessors apply.</p>
     *
     * @param request the completion to run
     * @return the request to hand a chat model
     */
    private static ChatRequest toChatRequest(final InferenceRequest request) {
        final ChatRequest.Builder builder = ChatRequest.builder()
                .messages(toChatMessages(request.messages()))
                .temperature(request.temperature())
                .maxOutputTokens(request.maxOutputTokens())
                .topP(request.topP());

        if (!request.stopSequences().isEmpty()) {
            builder.stopSequences(request.stopSequences());
        }
        if (request.hasTools()) {
            builder.toolSpecifications(toToolSpecifications(request.tools(), request.toolChoice()));
        }
        if (request.toolChoice() != null) {
            builder.toolChoice(toToolChoice(request.toolChoice()));
        }
        if (request.responseFormat() != null) {
            builder.responseFormat(toResponseFormat(request.responseFormat()));
        }
        return builder.build();
    }

    /**
     * Maps the conversation, turn by turn, preserving order and tool-call identity.
     *
     * @param messages the conversation, oldest first
     * @return the provider-facing messages
     */
    private static List<ChatMessage> toChatMessages(final List<InferenceMessage> messages) {
        final List<ChatMessage> chatMessages = new ArrayList<>(messages.size());
        for (final InferenceMessage message : messages) {
            chatMessages.add(toChatMessage(message));
        }
        return chatMessages;
    }

    /**
     * Maps one turn.
     *
     * @param message the turn
     * @return the provider-facing message
     */
    private static ChatMessage toChatMessage(final InferenceMessage message) {
        final Role role = message.role();
        if (role == Role.SYSTEM) {
            return new SystemMessage(nullToEmpty(message.content()));
        }
        if (role == Role.TOOL) {
            return new ToolExecutionResultMessage(
                    message.toolCallId(), message.name(), nullToEmpty(message.content()));
        }
        if (role == Role.ASSISTANT) {
            if (!message.hasToolCalls()) {
                return new AiMessage(nullToEmpty(message.content()));
            }
            final List<ToolExecutionRequest> toolRequests = new ArrayList<>(message.toolCalls().size());
            for (final InferenceToolCall toolCall : message.toolCalls()) {
                toolRequests.add(ToolExecutionRequest.builder()
                        .id(toolCall.id())
                        .name(toolCall.name())
                        .arguments(toolCall.arguments())
                        .build());
            }
            return message.content() == null
                    ? AiMessage.from(toolRequests)
                    : AiMessage.from(message.content(), toolRequests);
        }
        return new UserMessage(nullToEmpty(message.content()));
    }

    /**
     * Maps the declared tools.
     *
     * <p>When the caller forced one named tool, only that tool is offered. LangChain4J's shared
     * chat API has no "call this exact tool" choice — its strongest is "call some tool" — so
     * narrowing the offer is what turns that into the caller's actual instruction. If the forced
     * name matches nothing declared, every tool is offered and the provider is left to reject it,
     * which is a clearer failure than silently sending no tools at all.</p>
     *
     * @param tools      the declared tools
     * @param toolChoice the caller's preference, possibly null
     * @return the provider-facing tool specifications
     */
    private static List<ToolSpecification> toToolSpecifications(
            final List<InferenceToolSpec> tools,
            final com.dotcms.inference.model.ToolChoice toolChoice) {
        final boolean forcesOne = toolChoice != null
                && toolChoice.mode() == com.dotcms.inference.model.ToolChoice.Mode.FUNCTION;
        final boolean forcedIsDeclared = forcesOne
                && tools.stream().anyMatch(tool -> tool.name().equals(toolChoice.function()));

        final List<ToolSpecification> specifications = new ArrayList<>(tools.size());
        for (final InferenceToolSpec tool : tools) {
            if (forcedIsDeclared && !tool.name().equals(toolChoice.function())) {
                continue;
            }
            specifications.add(ToolSpecification.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .parameters(toObjectSchema(tool.parameters()))
                    .build());
        }
        return specifications;
    }

    /**
     * Maps the caller's tool preference.
     *
     * <p>{@code FUNCTION} becomes {@code REQUIRED}, paired with the narrowed tool list built by
     * {@link #toToolSpecifications}; together they say what the caller meant.</p>
     *
     * @param toolChoice the caller's preference
     * @return the provider-facing choice
     */
    private static dev.langchain4j.model.chat.request.ToolChoice toToolChoice(
            final com.dotcms.inference.model.ToolChoice toolChoice) {
        switch (toolChoice.mode()) {
            case NONE:
                return dev.langchain4j.model.chat.request.ToolChoice.NONE;
            case REQUIRED:
            case FUNCTION:
                return dev.langchain4j.model.chat.request.ToolChoice.REQUIRED;
            case AUTO:
            default:
                return dev.langchain4j.model.chat.request.ToolChoice.AUTO;
        }
    }

    /**
     * Maps the requested output shape.
     *
     * @param responseFormat the caller's requested shape
     * @return the provider-facing response format
     */
    private static dev.langchain4j.model.chat.request.ResponseFormat toResponseFormat(
            final com.dotcms.inference.model.ResponseFormat responseFormat) {
        switch (responseFormat.type()) {
            case JSON_OBJECT:
                return dev.langchain4j.model.chat.request.ResponseFormat.JSON;
            case JSON_SCHEMA:
                return dev.langchain4j.model.chat.request.ResponseFormat.builder()
                        .type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder()
                                .name(RESPONSE_SCHEMA_NAME)
                                .rootElement(toObjectSchema(responseFormat.schema()))
                                .build())
                        .build();
            case TEXT:
            default:
                return dev.langchain4j.model.chat.request.ResponseFormat.TEXT;
        }
    }

    /**
     * Wraps a caller-supplied JSON Schema in the object schema LangChain4J expects.
     *
     * <p>Only the envelope — properties, required, description, additionalProperties, definitions —
     * is read. Each property's schema is carried through verbatim as a raw element, because dotCMS
     * neither interprets nor validates it: the schema is the caller's contract with their own tool,
     * and re-modelling it here would quietly drop the keywords this mapping does not know about.</p>
     *
     * @param schema the caller's JSON Schema document
     * @return the equivalent object schema
     */
    private static JsonObjectSchema toObjectSchema(final JsonNode schema) {
        final JsonObjectSchema.Builder builder = JsonObjectSchema.builder();

        final JsonNode description = schema.get(SCHEMA_DESCRIPTION);
        if (description != null && description.isTextual()) {
            builder.description(description.asText());
        }

        final Map<String, JsonSchemaElement> properties = toRawElements(schema.get(SCHEMA_PROPERTIES));
        if (!properties.isEmpty()) {
            builder.addProperties(properties);
        }

        final JsonNode required = schema.get(SCHEMA_REQUIRED);
        if (required != null && required.isArray()) {
            final List<String> names = new ArrayList<>(required.size());
            required.forEach(name -> names.add(name.asText()));
            builder.required(names);
        }

        final JsonNode additionalProperties = schema.get(SCHEMA_ADDITIONAL_PROPERTIES);
        if (additionalProperties != null && additionalProperties.isBoolean()) {
            builder.additionalProperties(additionalProperties.asBoolean());
        }

        final JsonNode definitionsNode = schema.has(SCHEMA_DEFS)
                ? schema.get(SCHEMA_DEFS)
                : schema.get(SCHEMA_DEFINITIONS);
        final Map<String, JsonSchemaElement> definitions = toRawElements(definitionsNode);
        if (!definitions.isEmpty()) {
            builder.definitions(definitions);
        }

        return builder.build();
    }

    /**
     * Carries each member of a JSON object through as an uninterpreted schema element.
     *
     * @param node the object whose members are schemas, possibly null or not an object
     * @return the members keyed by name, in document order; empty if there are none
     */
    private static Map<String, JsonSchemaElement> toRawElements(final JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        final Map<String, JsonSchemaElement> elements = new LinkedHashMap<>();
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();
            elements.put(field.getKey(), JsonRawSchema.from(field.getValue().toString()));
        }
        return elements;
    }

    /**
     * Maps a finished provider response into the answer this family returns.
     *
     * @param response     the provider's response
     * @param servingModel the model that actually served, after any fallback hop
     * @return the completed answer
     */
    private static InferenceResponse toInferenceResponse(final ChatResponse response,
                                                         final String servingModel) {
        final AiMessage aiMessage = response.aiMessage();
        return new InferenceResponse(
                RESPONSE_ID_PREFIX + UUID.randomUUID().toString().replace("-", ""),
                servingModel,
                Instant.now().getEpochSecond(),
                toInferenceMessage(aiMessage),
                toFinishReason(response.finishReason(), aiMessage),
                toInferenceUsage(response.tokenUsage()));
    }

    /**
     * Maps the assistant's turn, carrying tool-call identities through untouched.
     *
     * @param aiMessage the provider's assistant message
     * @return the assistant turn
     */
    private static InferenceMessage toInferenceMessage(final AiMessage aiMessage) {
        if (aiMessage == null) {
            return InferenceMessage.of(Role.ASSISTANT, "");
        }
        if (!aiMessage.hasToolExecutionRequests()) {
            return InferenceMessage.of(Role.ASSISTANT, nullToEmpty(aiMessage.text()));
        }
        final List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
        final List<InferenceToolCall> toolCalls = new ArrayList<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            final ToolExecutionRequest request = requests.get(index);
            toolCalls.add(new InferenceToolCall(
                    request.id(), request.name(), request.arguments(), index));
        }
        return InferenceMessage.ofToolCalls(aiMessage.text(), toolCalls);
    }

    /**
     * Maps why generation stopped.
     *
     * <p>A provider that reports nothing is read from what it returned instead: an assistant turn
     * asking for tools stopped to have them executed, whatever the provider forgot to say.</p>
     *
     * @param finishReason the provider's reason, possibly null
     * @param aiMessage    the assistant turn, used when the provider was silent
     * @return the reason to report
     */
    private static com.dotcms.inference.model.FinishReason toFinishReason(
            final dev.langchain4j.model.output.FinishReason finishReason,
            final AiMessage aiMessage) {
        if (finishReason == null) {
            return aiMessage != null && aiMessage.hasToolExecutionRequests()
                    ? com.dotcms.inference.model.FinishReason.TOOL_CALLS
                    : com.dotcms.inference.model.FinishReason.STOP;
        }
        switch (finishReason) {
            case LENGTH:
                return com.dotcms.inference.model.FinishReason.LENGTH;
            case TOOL_EXECUTION:
                return com.dotcms.inference.model.FinishReason.TOOL_CALLS;
            case CONTENT_FILTER:
                return com.dotcms.inference.model.FinishReason.CONTENT_FILTER;
            case STOP:
            case OTHER:
            default:
                return com.dotcms.inference.model.FinishReason.STOP;
        }
    }

    /**
     * Maps reported token counts.
     *
     * @param tokenUsage the provider's counts, possibly null or partly absent
     * @return the counts, or {@link InferenceUsage#UNREPORTED} when the provider reported none
     */
    private static InferenceUsage toInferenceUsage(final TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return InferenceUsage.UNREPORTED;
        }
        final InferenceUsage usage = new InferenceUsage(
                tokenUsage.inputTokenCount(),
                tokenUsage.outputTokenCount(),
                tokenUsage.totalTokenCount());
        return usage.isReported() ? usage : InferenceUsage.UNREPORTED;
    }

    /**
     * Turns a failure into an error safe to hand a client.
     *
     * <p>A provider's own message is never passed through: it can quote the prompt back. Messages
     * dotCMS wrote — a misconfigured site, for instance — are safe and are kept, because they name
     * something the caller or an operator can actually fix.</p>
     *
     * @param throwable what went wrong
     * @return the error to report
     */
    static InferenceError toInferenceError(final Throwable throwable) {
        if (throwable instanceof CallerSafeException
                && !nullToEmpty(throwable.getMessage()).isBlank()) {
            // dotCMS wrote this sentence, so it is returned: it names something about the site's
            // configuration that the caller or an operator can actually act on.
            return InferenceError.invalidRequest(throwable.getMessage(), null);
        }
        if (throwable instanceof IllegalArgumentException) {
            // dotCMS did not write this one. The fallback chain reports a failed model
            // initialisation as an IllegalArgumentException with the provider client's own message
            // appended, and that message can carry the provider's endpoint, its account
            // identifiers, or a fragment of the prompt. Logged in full, never returned.
            return InferenceError.invalidRequest(
                    "The request could not be served with this site's configuration", null);
        }
        // A streamed exchange is rate limited exactly as often as a buffered one, and a client
        // reading an error event backs off on the same status, so the translation belongs here too.
        return InferenceError.fromProviderFailure(throwable, UPSTREAM_FAILURE_MESSAGE);
    }

    /**
     * @param value a possibly null string
     * @return the value, or an empty string
     */
    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }

    /**
     * Reads an image as base64, fetching it first when the provider only gave a link to it.
     *
     * @param image the provider's image
     * @return the image bytes, base64 encoded
     */
    private static String toBase64(final Image image) {
        if (image == null) {
            throw new IllegalStateException(NO_IMAGE_MESSAGE);
        }
        if (image.base64Data() != null && !image.base64Data().isBlank()) {
            return image.base64Data();
        }
        if (image.url() == null) {
            throw new IllegalStateException(NO_IMAGE_MESSAGE);
        }
        return fetchAndEncode(image.url());
    }

    /**
     * Downloads a provider-hosted image and encodes it.
     *
     * <p>Bounded by {@link #IMAGE_FETCH_TIMEOUT_SECONDS} so a provider whose CDN hangs cannot park
     * the request thread for as long as it likes. The address is the provider's own, taken from a
     * response to a request dotCMS made to an endpoint the site configured, and it is never logged
     * or handed back to the caller.</p>
     *
     * @param url where the provider put the image
     * @return the image bytes, base64 encoded
     */
    private static String fetchAndEncode(final URI url) {
        final Duration timeout = Duration.ofSeconds(IMAGE_FETCH_TIMEOUT_SECONDS);
        try {
            final HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            final HttpResponse<byte[]> response = httpClient.send(
                    HttpRequest.newBuilder(url).timeout(timeout).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() / 100 != 2
                    || response.body() == null
                    || response.body().length == 0) {
                throw new IllegalStateException(NO_IMAGE_MESSAGE);
            }
            return Base64.getEncoder().encodeToString(response.body());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(NO_IMAGE_MESSAGE, e);
        } catch (final IOException e) {
            Logger.warn(InferenceAIClient.class,
                    "Could not retrieve the provider's generated image: "
                            + e.getClass().getSimpleName());
            throw new IllegalStateException(NO_IMAGE_MESSAGE, e);
        }
    }

    /**
     * Maps the token counts an embeddings exchange reported.
     *
     * <p>An embeddings call generates nothing, so there is no completion count to report and the
     * total equals the prompt count. A provider that reported only the prompt count is therefore
     * read as having reported the total too — that is arithmetic on what it said, not an estimate
     * of what it did not.</p>
     *
     * @param tokenUsage the provider's counts, possibly null or partly absent
     * @return the counts, or {@link InferenceUsage#UNREPORTED} when the provider reported none
     */
    private static InferenceUsage toEmbeddingUsage(final TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return InferenceUsage.UNREPORTED;
        }
        final Integer inputTokens = tokenUsage.inputTokenCount();
        final Integer totalTokens = tokenUsage.totalTokenCount() == null
                ? inputTokens
                : tokenUsage.totalTokenCount();
        final InferenceUsage usage = new InferenceUsage(inputTokens, null, totalTokens);
        return usage.isReported() ? usage : InferenceUsage.UNREPORTED;
    }

    /**
     * One batch of embeddings, as the provider produced them.
     *
     * @param model   the model that actually served, after any fallback hop
     * @param vectors one vector per input, in the order the inputs were given
     * @param usage   tokens consumed by the whole batch
     */
    public record EmbeddingBatch(String model, List<List<Float>> vectors, InferenceUsage usage) {
    }

    /**
     * The images one generation request produced.
     *
     * @param model  the model that actually served, after any fallback hop
     * @param images the images, in the order the provider returned them
     */
    public record GeneratedImages(String model, List<GeneratedImage> images) {
    }

    /**
     * One generated image, always as base64.
     *
     * @param base64Data    the image bytes, base64 encoded
     * @param revisedPrompt the prompt the provider says it actually used, when it says so
     */
    public record GeneratedImage(String base64Data, String revisedPrompt) {
    }

    /**
     * Guards the one guarantee a streamed completion makes: exactly one terminal event.
     *
     * <p>Provider callbacks arrive on the provider's own threads, so every emission is serialized
     * here and the terminal flag is checked under the same lock. That is also what keeps a timeout
     * racing a late completion from producing both an error and a finish.</p>
     */
    private static final class StreamState {

        private final Consumer<InferenceStreamEvent> sink;
        private final boolean includeUsage;
        private final CountDownLatch terminalLatch = new CountDownLatch(1);
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final AtomicBoolean emitted = new AtomicBoolean();
        private final Set<Integer> fragmentedToolCalls = Collections.synchronizedSet(new HashSet<>());

        private StreamState(final Consumer<InferenceStreamEvent> sink, final boolean includeUsage) {
            this.sink = sink;
            this.includeUsage = includeUsage;
        }

        /** @return whether anything has reached the sink yet */
        private boolean hasEmitted() {
            return emitted.get();
        }

        /**
         * @param timeoutSeconds the ceiling on the whole completion
         * @return whether a terminal event was reached within the ceiling
         */
        private boolean awaitTerminal(final int timeoutSeconds) {
            try {
                return terminalLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(InferenceError.upstream("The completion was interrupted before it finished"));
                return true;
            }
        }

        /**
         * @param token a fragment of the answer's text
         */
        private void contentDelta(final String token) {
            emit(new InferenceStreamEvent.ContentDelta(token));
        }

        /**
         * @param partialToolCall a fragment of one tool call
         */
        private void partialToolCall(final PartialToolCall partialToolCall) {
            // The provider repeats the id and name on every fragment of a call. The wire format
            // does not: identity is announced once, and a client seeing an id again reads it as a
            // second tool call starting. So only the first fragment of each index carries them.
            final boolean firstFragment = fragmentedToolCalls.add(partialToolCall.index());
            emit(new InferenceStreamEvent.ToolCallDelta(
                    partialToolCall.index(),
                    firstFragment ? partialToolCall.id() : null,
                    firstFragment ? partialToolCall.name() : null,
                    partialToolCall.partialArguments()));
        }

        /**
         * Emits a whole tool call as one fragment, but only for a provider that never streamed it.
         *
         * <p>Providers that do stream tool calls send this after the fragments, and re-emitting the
         * arguments here would have a reader concatenate them twice into malformed JSON.</p>
         *
         * @param completeToolCall the finished tool call
         */
        private void completeToolCall(final CompleteToolCall completeToolCall) {
            if (fragmentedToolCalls.contains(completeToolCall.index())) {
                return;
            }
            final ToolExecutionRequest request = completeToolCall.toolExecutionRequest();
            emit(new InferenceStreamEvent.ToolCallDelta(
                    completeToolCall.index(), request.id(), request.name(), request.arguments()));
        }

        /**
         * Ends the stream normally, with the usage event only if the caller asked for one.
         *
         * @param response the provider's finished response
         */
        private synchronized void finish(final ChatResponse response) {
            if (terminated.get()) {
                return;
            }
            final AiMessage aiMessage = response == null ? null : response.aiMessage();
            emit(new InferenceStreamEvent.Finish(toFinishReason(
                    response == null ? null : response.finishReason(), aiMessage)));
            if (includeUsage) {
                emit(new InferenceStreamEvent.Usage(
                        toInferenceUsage(response == null ? null : response.tokenUsage())));
            }
            terminate();
        }

        /**
         * Ends the stream as failed. Ignored once the stream has already ended, so a finish and an
         * error can never both be reported.
         *
         * @param error what went wrong
         */
        private synchronized void fail(final InferenceError error) {
            if (terminated.get()) {
                return;
            }
            emit(new InferenceStreamEvent.Error(error));
            terminate();
        }

        /**
         * Hands one event to the sink.
         *
         * <p>A sink that throws is a caller that has gone away — a disconnected client, typically.
         * There is nowhere left to report anything, so the stream is ended silently and the waiting
         * thread released rather than held until the timeout.</p>
         *
         * @param event the event to emit
         */
        private synchronized void emit(final InferenceStreamEvent event) {
            if (terminated.get()) {
                return;
            }
            try {
                sink.accept(event);
                emitted.set(true);
            } catch (final RuntimeException e) {
                Logger.warn(InferenceAIClient.class,
                        "Inference stream consumer rejected an event, ending the stream: "
                                + e.getClass().getSimpleName());
                terminate();
            }
        }

        /** Marks the stream ended and releases whoever is waiting on it. */
        private void terminate() {
            terminated.set(true);
            terminalLatch.countDown();
        }
    }
}
