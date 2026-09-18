package com.dotcms.ai.client.langchain4j;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.inference.model.CallerSafeException;
import com.dotcms.ai.client.AIClient;
import com.dotcms.ai.client.AIRequest;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.domain.AIProvider;
import com.dotcms.ai.exception.DotAIAppConfigDisabledException;
import com.dotcms.ai.exception.DotAIClientConnectException;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import io.vavr.Lazy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link AIClient} implementation backed by LangChain4J.
 *
 * <p>Replaces the custom OpenAI HTTP client ({@code OpenAIClient}) with a unified LangChain4J
 * abstraction layer that supports multiple AI providers without custom HTTP handling.
 *
 * <p>Model instances are cached per host and provider configuration to avoid rebuilding
 * them on every request. The cache key is {@code hostname:configHash:type} where
 * {@code configHash} is the SHA-256 hex digest of the {@code providerConfig} JSON (credentials
 * are never stored in heap keys) and {@code type} is {@code chat}, {@code embeddings},
 * or {@code image}.
 *
 * <p>The response JSON is formatted in OpenAI-compatible structure so that all
 * existing upper-layer code ({@code CompletionsAPIImpl}, {@code EmbeddingsAPIImpl}, etc.)
 * can parse it without modification.
 */
public class LangChain4jAIClient implements AIClient {

    private static final Lazy<LangChain4jAIClient> INSTANCE = Lazy.of(LangChain4jAIClient::new);
    private static final ObjectMapper MAPPER = DotObjectMapperProvider.createDefaultMapper();
    private static final long MODEL_CACHE_TTL_HOURS = 1;
    private static final long STREAMING_TIMEOUT_SECONDS = 300;
    private static final String CHAT_SECTION = "chat";
    private static final String EMBEDDINGS_SECTION = "embeddings";
    private static final String IMAGE_SECTION = "image";

    /**
     * Response format asked of an image provider by the {@code /api/inference/v1} family, so the
     * bytes arrive inline and no separately-addressable artifact is minted upstream.
     */

    /**
     * Cache-key discriminator for models the inference family borrows. Those differ from the ones
     * the legacy endpoints get — they are built asking for the inline image form — so they cannot
     * share a cache entry with them even for the same site, model and size.
     */
    private static final String INFERENCE_KEY_SEGMENT = ":inference";

    private final Cache<String, ChatModel> chatModelCache = Caffeine.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(MODEL_CACHE_TTL_HOURS, TimeUnit.HOURS)
            .build();
    private final Cache<String, StreamingChatModel> streamingChatModelCache = Caffeine.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(MODEL_CACHE_TTL_HOURS, TimeUnit.HOURS)
            .build();
    private final Cache<String, EmbeddingModel> embeddingModelCache = Caffeine.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(MODEL_CACHE_TTL_HOURS, TimeUnit.HOURS)
            .build();
    private final Cache<String, ImageModel> imageModelCache = Caffeine.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(MODEL_CACHE_TTL_HOURS, TimeUnit.HOURS)
            .build();

    private LangChain4jAIClient() {}

    public static LangChain4jAIClient get() {
        return INSTANCE.get();
    }

    /**
     * Evicts cached model instances for the specified host. Should be called when the provider
     * config for a host changes (e.g., API key rotation) to ensure stale credentials are not reused.
     *
     * <p>Cache keys are prefixed with the hostname, so only entries for the affected host
     * are invalidated — other hosts' cached models are unaffected.
     *
     * @param hostname the hostname whose cached models should be evicted
     */
    public void flushCachesForHost(final String hostname) {
        final String prefix = hostname + ":";
        chatModelCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        streamingChatModelCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        embeddingModelCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        imageModelCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Hands a caller a chat model for a site, with the fallback chain and cache already applied.
     *
     * <p>Exists so the {@code /api/inference/v1} family can own its own request and response
     * semantics — which are dictated by an external standard and will change when that standard
     * changes — without owning model construction, caching or eviction. Those stay here, in one
     * place, for one reason: {@link #flushCachesForHost(String)} is what evicts a site's cached
     * providers when its credentials are rotated, and it is wired to this class alone
     * ({@code AIAppListener}). A second client holding its own cache would keep serving a revoked
     * key until the TTL expired, with no symptom to notice.</p>
     *
     * <p>The executor receives the model's name alongside the model because a fallback hop means
     * the model that served is not the model that was asked for, and a caller reporting back to a
     * client has to say which one actually ran.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param executor  receives a chat model and its name, and produces the result
     * @param <R>       the result type
     * @return whatever the executor returned for the first model that succeeded
     */
    public <R> R withChatModel(final AppConfig appConfig,
                               final String requestedModel,
                               final BiFunction<ChatModel, String, R> executor) {
        return executeForModels(
                cacheKeyPrefix(appConfig),
                CHAT_SECTION,
                onlyModel(requestedModel),
                parseSection(appConfig.getProviderConfig(), CHAT_SECTION),
                chatModelCache,
                LangChain4jModelFactory::buildChatModel,
                executor);
    }

    /**
     * Hands a caller a streaming chat model for a site, with the fallback chain and cache applied.
     *
     * <p>The streaming counterpart of {@link #withChatModel}; see that method for why model
     * acquisition stays in this class rather than moving to the caller.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param executor  receives a streaming chat model and its name
     */
    public void withStreamingChatModel(final AppConfig appConfig,
                                       final String requestedModel,
                                       final BiConsumer<StreamingChatModel, String> executor) {
        executeForModels(
                cacheKeyPrefix(appConfig),
                CHAT_SECTION,
                onlyModel(requestedModel),
                parseSection(appConfig.getProviderConfig(), CHAT_SECTION),
                streamingChatModelCache,
                LangChain4jModelFactory::buildStreamingChatModel,
                (model, modelName) -> {
                    executor.accept(model, modelName);
                    return null;
                });
    }

    /**
     * Hands a caller an embedding model for a site, with the fallback chain and cache applied.
     *
     * <p>The embeddings counterpart of {@link #withChatModel}; see that method for why model
     * acquisition stays in this class rather than moving to the caller. The model is built from
     * the site's {@code embeddings} section, which a site configures independently of its chat
     * models.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param executor  receives an embedding model and its name, and produces the result
     * @param <R>       the result type
     * @return whatever the executor returned for the first model that succeeded
     */
    public <R> R withEmbeddingModel(final AppConfig appConfig,
                                    final String requestedModel,
                                    final BiFunction<EmbeddingModel, String, R> executor) {
        return executeForModels(
                cacheKeyPrefix(appConfig),
                EMBEDDINGS_SECTION,
                onlyModel(requestedModel),
                parseSection(appConfig.getProviderConfig(), EMBEDDINGS_SECTION),
                embeddingModelCache,
                LangChain4jModelFactory::buildEmbeddingModel,
                executor);
    }

    /**
     * Hands a caller an image model for a site, asked for the inline image form.
     *
     * <p>The image counterpart of {@link #withChatModel}, with two differences that are the
     * caller's request rather than the site's configuration. The requested {@code size} is applied
     * over the configured one, because a caller who named a size changed both what they receive
     * and what the site pays.</p>
     *
     * <p><strong>The output-format parameter is deliberately not sent.</strong> Asking for the
     * inline form was the obvious way to stop a provider minting a hosted artifact, and it is what
     * this did first — but the current generation of image models rejects the parameter outright
     * ("Unknown parameter: 'response_format'"), because they only ever return base64 and there is
     * nothing to choose. Sending it broke image generation against every one of them while working
     * against the older models this family's tests stubbed. Not sending it is both simpler and
     * safer than naming the models that accept it: the set that rejects it grows with every
     * release, so any list would rot. Where a legacy model returns a URL instead, the caller still
     * receives base64 — the answer is fetched and re-encoded, which this family already does.</p>
     *
     * <p>Both are folded into the cache key, so a model asked for one size is never handed to a
     * request that asked for another, and the legacy image endpoint — which wants the provider's
     * own default format — never receives one of these.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param size      the size the caller asked for, or null/blank to use the configured one
     * @param executor  receives an image model and its name, and produces the result
     * @param <R>       the result type
     * @return whatever the executor returned for the first model that succeeded
     */
    public <R> R withImageModel(final AppConfig appConfig,
                                final String requestedModel,
                                final String size,
                                final BiFunction<ImageModel, String, R> executor) {
        final ProviderConfig baseConfig = parseSection(appConfig.getProviderConfig(), IMAGE_SECTION);
        final boolean sized = size != null && !size.isBlank();
        final ProviderConfig requestConfig = ImmutableProviderConfig.copyOf(baseConfig)
                .withSize(sized ? size : baseConfig.size());

        return executeForModels(
                cacheKeyPrefix(appConfig) + INFERENCE_KEY_SEGMENT
                        + (requestConfig.size() == null ? "" : ":" + requestConfig.size()),
                IMAGE_SECTION,
                onlyModel(requestedModel),
                requestConfig,
                imageModelCache,
                LangChain4jModelFactory::buildImageModel,
                executor);
    }

    /**
     * Reads the model names a site has configured for one section of its {@code providerConfig}.
     *
     * <p>Exists so that the model gate every {@code /api/inference/v1} endpoint applies, and the
     * listing that tells a caller what will pass it, read the configuration through the class that
     * owns it rather than each re-implementing the same parse. Fallback chains are returned whole
     * and in configured order, because every entry is a name the gate accepts.</p>
     *
     * <p>A site with no usable configuration for that section yields an empty list rather than an
     * exception. From where a caller stands, a model nobody configured and a section nobody
     * configured are the same absence, and distinguishing them would disclose which sites have
     * dotAI set up.</p>
     *
     * @param appConfig the resolved site's configuration
     * @param section   the {@code providerConfig} section, e.g. {@code chat}
     * @return the configured model names in fallback order; empty when there are none
     */
    public List<String> configuredModels(final AppConfig appConfig, final String section) {
        final String providerConfigJson = appConfig == null ? null : appConfig.getProviderConfig();
        if (providerConfigJson == null || providerConfigJson.isBlank()) {
            return List.of();
        }
        try {
            final JsonNode sectionNode = MAPPER.readTree(providerConfigJson).get(section);
            if (sectionNode == null || sectionNode.isNull()) {
                return List.of();
            }
            return List.copyOf(
                    effectiveModels(MAPPER.treeToValue(sectionNode, ProviderConfig.class)));
        } catch (final Exception e) {
            // Never the parser's message: providerConfig carries credentials and a parse failure
            // can quote the fragment it choked on.
            Logger.warn(LangChain4jAIClient.class, "Could not read the '" + section
                    + "' section of providerConfig: " + e.getClass().getSimpleName());
            return List.of();
        }
    }

    /**
     * The cache key prefix for a site's models.
     *
     * <p>Derived from the configuration rather than from the request, which is what makes two
     * sites with different providers get separate model instances for free, and what makes a
     * credential rotation change the key so the old instance is no longer reachable.</p>
     *
     * @param appConfig the resolved site's configuration
     * @return the prefix shared by every cache entry for that site and configuration
     */
    private static String cacheKeyPrefix(final AppConfig appConfig) {
        return appConfig.getHost() + ":" + appConfig.getProviderConfigHash();
    }

    @Override
    public AIProvider getProvider() {
        return AIProvider.LANGCHAIN4J;
    }

    /**
     * Executes the AI request and writes an OpenAI-compatible JSON response to {@code output}.
     *
     * <p>Routing is determined by {@link AIModelType} in the request:
     * <ul>
     *   <li>{@code TEXT} → chat completion</li>
     *   <li>{@code EMBEDDINGS} → embedding generation</li>
     *   <li>{@code IMAGE} → image generation</li>
     * </ul>
     */
    @Override
    public <T extends Serializable> void sendRequest(final AIRequest<T> request, final OutputStream output) {
        final JSONObjectAIRequest jsonRequest = AIClient.useRequestOrThrow(request);
        final AppConfig appConfig = jsonRequest.getConfig();

        if (!appConfig.isEnabled()) {
            throw new DotAIAppConfigDisabledException("App dotAI config is not enabled — set providerConfig");
        }

        final String providerConfigJson = appConfig.getProviderConfig();
        final AIModelType type = jsonRequest.getType();
        final JSONObject payload = jsonRequest.getPayload();

        AppConfig.debugLogger(appConfig, LangChain4jAIClient.class,
                () -> "LangChain4jAIClient: type=" + type + " payload=" + payload.toString(2));

        final String cacheKeyPrefix = appConfig.getHost() + ":" + appConfig.getProviderConfigHash();

        if (type == AIModelType.IMAGE) {
            writeToOutput(executeImageRequest(cacheKeyPrefix, providerConfigJson, payload), output);
        } else if (type == AIModelType.EMBEDDINGS) {
            writeToOutput(executeEmbeddingRequest(cacheKeyPrefix, providerConfigJson, payload), output);
        } else if (Boolean.TRUE.equals(payload.opt(AiKeys.STREAM))) {
            executeStreamingChatRequest(cacheKeyPrefix, providerConfigJson, payload, output);
        } else {
            writeToOutput(executeChatRequest(cacheKeyPrefix, providerConfigJson, payload), output);
        }
    }

    private String executeChatRequest(final String cacheKeyPrefix, final String providerConfigJson, final JSONObject payload) {
        final ProviderConfig baseConfig = parseSection(providerConfigJson, "chat");
        final List<ChatMessage> messages = toMessages(payload.optJSONArray(AiKeys.MESSAGES));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Chat request must contain at least one message");
        }
        return executeWithFallback(cacheKeyPrefix, "chat", baseConfig, chatModelCache,
                LangChain4jModelFactory::buildChatModel,
                model -> toChatResponseJson(model.chat(ChatRequest.builder().messages(messages).build())));
    }

    private void executeStreamingChatRequest(final String cacheKeyPrefix,
                                             final String providerConfigJson,
                                             final JSONObject payload,
                                             final OutputStream output) {
        final ProviderConfig baseConfig = parseSection(providerConfigJson, "chat");
        final List<String> models = effectiveModels(baseConfig);
        if (models.isEmpty()) {
            throw new IllegalArgumentException("No model configured in providerConfig.chat — set 'model'");
        }

        final List<ChatMessage> messages = toMessages(payload.optJSONArray(AiKeys.MESSAGES));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Chat request must contain at least one message");
        }

        final StreamingChatModel model = initStreamingModel(cacheKeyPrefix, baseConfig, models);
        streamWithModel(model, messages, output);
    }

    // Fallback is only possible before streaming starts — once bytes are written to output
    // we cannot retry. Each init failure is logged immediately; the last exception is
    // rethrown only after all configured fallback models have been attempted.
    private StreamingChatModel initStreamingModel(
            final String cacheKeyPrefix,
            final ProviderConfig baseConfig,
            final List<String> models) {
        RuntimeException lastException = null;
        for (final String modelName : models) {
            try {
                final ProviderConfig modelConfig = ImmutableProviderConfig.copyOf(baseConfig).withModel(modelName);
                return streamingChatModelCache.get(
                        cacheKeyPrefix + ":chat:streaming:" + modelName,
                        k -> LangChain4jModelFactory.buildStreamingChatModel(modelConfig));
            } catch (RuntimeException e) {
                lastException = new IllegalArgumentException(
                        "Failed to initialize streaming model '" + modelName + "': " + e.getMessage(), e);
                Logger.warn(LangChain4jAIClient.class,
                        "Streaming model '" + modelName + "' init failed: " + e.getMessage()
                        + (models.size() > 1 ? " — trying next model" : ""));
            }
        }
        throw lastException != null ? lastException
                : new IllegalArgumentException("All configured streaming chat models exhausted");
    }

    private void streamWithModel(final StreamingChatModel model,
                                 final List<ChatMessage> messages,
                                 final OutputStream output) {
        final ChatRequest chatRequest = ChatRequest.builder().messages(messages).build();
        final long start = System.currentTimeMillis();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicReference<Boolean> cancelled = new AtomicReference<>(false);

        model.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(final String token) {
                if (cancelled.get()) {
                    return;
                }
                try {
                    output.write(toSseChunk(token).getBytes(StandardCharsets.UTF_8));
                    output.flush();
                } catch (IOException e) {
                    cancelled.set(true);
                    error.set(e);
                    latch.countDown();
                }
            }

            @Override
            public void onCompleteResponse(final ChatResponse response) {
                try {
                    output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    Logger.warn(LangChain4jAIClient.class, "Failed to write [DONE] marker: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onError(final Throwable e) {
                error.set(e);
                latch.countDown();
            }
        });

        try {
            final boolean completed = latch.await(STREAMING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                cancelled.set(true);
                throw new DotAIClientConnectException(
                        "Streaming timed out after " + STREAMING_TIMEOUT_SECONDS + " seconds",
                        new java.util.concurrent.TimeoutException());
            }
            Logger.info(LangChain4jAIClient.class,
                    "Streaming chat completed in " + (System.currentTimeMillis() - start) + "ms");
        } catch (InterruptedException e) {
            cancelled.set(true);
            Thread.currentThread().interrupt();
            throw new DotAIClientConnectException("Streaming interrupted: " + e.getMessage(), e);
        }

        if (error.get() != null) {
            final Throwable t = error.get();
            throw new DotAIClientConnectException("Streaming failed: " + t.getMessage(), t);
        }
    }

    private static String toSseChunk(final String token) {
        final JSONObject delta = new JSONObject();
        delta.put(AiKeys.CONTENT, token);
        final JSONObject choice = new JSONObject();
        choice.put("delta", delta);
        choice.put(AiKeys.INDEX, 0);
        final JSONArray choices = new JSONArray();
        choices.put(choice);
        final JSONObject chunk = new JSONObject();
        chunk.put("choices", choices);
        return "data: " + chunk + "\n\n";
    }

    private void writeToOutput(final String responseJson, final OutputStream output) {
        try {
            output.write(responseJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(this, "Failed to write AI response to output stream: " + e.getMessage(), e);
            throw new DotAIClientConnectException("Failed to write AI response to output stream: " + e.getMessage(), e);
        }
    }

    private String executeEmbeddingRequest(final String cacheKeyPrefix, final String providerConfigJson, final JSONObject payload) {
        final ProviderConfig baseConfig = parseSection(providerConfigJson, "embeddings");
        final String input = payload.getString(AiKeys.INPUT);
        return executeWithFallback(cacheKeyPrefix, "embeddings", baseConfig, embeddingModelCache,
                LangChain4jModelFactory::buildEmbeddingModel,
                model -> toEmbeddingResponseJson(model.embed(TextSegment.from(input)).content()));
    }

    private String executeImageRequest(final String cacheKeyPrefix, final String providerConfigJson, final JSONObject payload) {
        final ProviderConfig baseConfig = parseSection(providerConfigJson, "image");
        final String prompt = payload.getString(AiKeys.PROMPT);
        final ProviderConfig imageConfig = applyRequestSize(baseConfig, payload);
        final String sizeSuffix = imageConfig.size() != null ? ":" + imageConfig.size() : "";
        return executeWithFallback(cacheKeyPrefix + sizeSuffix, "image", imageConfig, imageModelCache,
                LangChain4jModelFactory::buildImageModel,
                model -> toImageResponseJson(model.generate(prompt).content()));
    }

    @VisibleForTesting
    static ProviderConfig applyRequestSize(final ProviderConfig baseConfig, final JSONObject payload) {
        final String size = payload.optString(AiKeys.SIZE, null);
        if (size != null && !size.isBlank()) {
            return ImmutableProviderConfig.copyOf(baseConfig).withSize(size);
        }
        return baseConfig;
    }

    @VisibleForTesting
    <M> String executeWithFallback(
            final String cacheKeyPrefix,
            final String section,
            final ProviderConfig baseConfig,
            final Cache<String, M> modelCache,
            final Function<ProviderConfig, M> modelBuilder,
            final Function<M, String> executor) {
        return executeWithFallbackTyped(cacheKeyPrefix, section, baseConfig, modelCache, modelBuilder,
                (model, modelName) -> executor.apply(model));
    }

    /**
     * Runs {@code executor} against each configured model in turn until one succeeds.
     *
     * <p>Generalised from the String-returning variant above, which now delegates here, so the
     * fallback chain, the cache keying and the per-attempt logging have exactly one
     * implementation. The executor additionally receives the name of the model it was handed,
     * because a caller reporting results back to a client needs to say which model actually
     * served — after a fallback hop that is not the one the caller asked for.</p>
     *
     * @param cacheKeyPrefix the site-and-config-derived cache key prefix
     * @param section        the providerConfig section, e.g. {@code chat}
     * @param baseConfig     the parsed section config
     * @param modelCache     the cache for this model type
     * @param modelBuilder   builds a model from a config naming one model
     * @param executor       receives the model and the model's name, and produces the result
     * @param <M>            the provider model type
     * @param <R>            the result type
     * @return the first successful result
     */
    /**
     * The single model an {@code /api/inference/v1} request may run.
     *
     * <p>Wrapped as a one-element chain rather than handed to a separate code path, so that model
     * construction, caching and failure reporting stay identical to the fallback case — the only
     * difference is that there is nothing to fall back to.</p>
     *
     * @param requestedModel the model the caller named, already checked against the site's set
     * @return that model alone
     */
    private static List<String> onlyModel(final String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            // Reached only if a caller skipped the model gate. Refusing beats quietly running
            // whatever the site happens to list first, which is the behaviour this replaced.
            throw new CallerSafeException("A model is required; this endpoint selects no default");
        }
        return List.of(requestedModel);
    }

    <M, R> R executeWithFallbackTyped(
            final String cacheKeyPrefix,
            final String section,
            final ProviderConfig baseConfig,
            final Cache<String, M> modelCache,
            final Function<ProviderConfig, M> modelBuilder,
            final BiFunction<M, String, R> executor) {
        return executeForModels(cacheKeyPrefix, section, effectiveModels(baseConfig), baseConfig,
                modelCache, modelBuilder, executor);
    }

    /**
     * Runs the executor against a named list of models, in order, stopping at the first success.
     *
     * <p>Separated from {@link #executeWithFallbackTyped} so a caller can say which models may run
     * rather than inheriting the site's chain. The {@code /api/inference/v1} family passes exactly
     * one — the model the caller asked for — because a fallback there would spend a developer's
     * money on a model they did not choose and cannot see in the response. The legacy endpoints
     * keep the chain, where resilience is worth more than that certainty.</p>
     *
     * @param cacheKeyPrefix the site- and config-specific key prefix
     * @param section        the provider config section being drawn from
     * @param models         the models to try, in order; a single entry means no fallback
     * @param baseConfig     the section's configuration
     * @param modelCache     the cache for this model type
     * @param modelBuilder   builds a model from a configuration
     * @param executor       receives a model and its name, and produces the result
     * @param <M>            the model type
     * @param <R>            the result type
     * @return whatever the executor returned for the first model that succeeded
     */
    <M, R> R executeForModels(
            final String cacheKeyPrefix,
            final String section,
            final List<String> models,
            final ProviderConfig baseConfig,
            final Cache<String, M> modelCache,
            final Function<ProviderConfig, M> modelBuilder,
            final BiFunction<M, String, R> executor) {
        if (models.isEmpty()) {
            // CallerSafeException, not a bare IllegalArgumentException: this sentence was written
            // here, names the site's own configuration, and is the one thing that tells a caller
            // what to fix. The type is what marks it returnable — see CallerSafeException.
            throw new CallerSafeException(
                    "No model configured in providerConfig." + section + " — set 'model'");
        }
        // Each failure is logged immediately. The last exception is rethrown only after
        // all configured fallback models have been attempted.
        RuntimeException lastException = null;
        for (final String modelName : models) {
            final ProviderConfig modelConfig = ImmutableProviderConfig.copyOf(baseConfig).withModel(modelName);
            final M model;
            try {
                model = modelCache.get(
                        cacheKeyPrefix + ":" + section + ":" + modelName,
                        k -> modelBuilder.apply(modelConfig));
            } catch (RuntimeException e) {
                lastException = new IllegalArgumentException(
                        "Failed to initialize " + section + " model '" + modelName + "': " + e.getMessage(), e);
                Logger.warn(LangChain4jAIClient.class,
                        section + " model '" + modelName + "' init failed: " + e.getMessage()
                        + (models.size() > 1 ? " — trying next model" : ""));
                continue;
            }
            try {
                final long start = System.currentTimeMillis();
                final R result = executor.apply(model, modelName);
                Logger.info(LangChain4jAIClient.class,
                        section + " model '" + modelName + "' responded in "
                        + (System.currentTimeMillis() - start) + "ms");
                return result;
            } catch (RuntimeException e) {
                lastException = e;
                Logger.warn(LangChain4jAIClient.class,
                        section + " model '" + modelName + "' failed: " + e.getMessage()
                        + (models.size() > 1 ? " — trying next model" : ""));
            }
        }
        throw lastException != null ? lastException
                : new IllegalArgumentException("All configured " + section + " models exhausted");
    }

    static List<ChatMessage> toMessages(final JSONArray messagesArray) {
        final List<ChatMessage> messages = new ArrayList<>();
        if (messagesArray == null) {
            return messages;
        }
        for (int i = 0; i < messagesArray.length(); i++) {
            final JSONObject msg = messagesArray.getJSONObject(i);
            final String role = msg.optString(AiKeys.ROLE, AiKeys.USER).toLowerCase();
            final Object contentRaw = msg.opt(AiKeys.CONTENT);
            if ("system".equals(role)) {
                messages.add(new SystemMessage(contentRaw != null ? contentRaw.toString() : ""));
            } else if ("assistant".equals(role)) {
                messages.add(new AiMessage(contentRaw != null ? contentRaw.toString() : ""));
            } else if (contentRaw instanceof JSONArray) {
                messages.add(toMultimodalUserMessage((JSONArray) contentRaw));
            } else {
                messages.add(new UserMessage(contentRaw != null ? contentRaw.toString() : ""));
            }
        }
        return messages;
    }

    static UserMessage toMultimodalUserMessage(final JSONArray contentParts) {
        final List<Content> parts = new ArrayList<>();
        for (int i = 0; i < contentParts.length(); i++) {
            final JSONObject part = contentParts.getJSONObject(i);
            parts.add("image_url".equals(part.optString("type", "text"))
                    ? toImageContent(part)
                    : TextContent.from(part.optString("text", "")));
        }
        return UserMessage.from(parts);
    }

    private static Content toImageContent(final JSONObject part) {
        final JSONObject imageUrlObj = part.optJSONObject("image_url");
        final String url = imageUrlObj != null ? imageUrlObj.optString("url", "") : part.optString("image_url", "");
        if (url.startsWith("data:")) {
            final int semicolon = url.indexOf(';');
            final int comma = url.indexOf(',');
            if (semicolon > 0 && comma > semicolon) {
                return ImageContent.from(url.substring(comma + 1), url.substring(5, semicolon));
            }
        }
        return ImageContent.from(url);
    }

    static String toChatResponseJson(final ChatResponse response) {
        final JSONObject message = new JSONObject();
        message.put(AiKeys.ROLE, "assistant");
        message.put(AiKeys.CONTENT, response.aiMessage().text());

        final FinishReason finishReason = response.finishReason();
        final JSONObject choice = new JSONObject();
        choice.put(AiKeys.MESSAGE, message);
        choice.put(AiKeys.INDEX, 0);
        choice.put("finish_reason", finishReason != null ? finishReason.name().toLowerCase() : "stop");
        choice.put("logprobs", JSONObject.NULL);

        final JSONArray choices = new JSONArray();
        choices.put(choice);

        final TokenUsage tokenUsage = response.tokenUsage();
        final JSONObject usage = new JSONObject();
        usage.put("prompt_tokens", tokenUsage != null && tokenUsage.inputTokenCount() != null ? tokenUsage.inputTokenCount() : 0);
        usage.put("completion_tokens", tokenUsage != null && tokenUsage.outputTokenCount() != null ? tokenUsage.outputTokenCount() : 0);
        usage.put("total_tokens", tokenUsage != null && tokenUsage.totalTokenCount() != null ? tokenUsage.totalTokenCount() : 0);

        final JSONObject result = new JSONObject();
        result.put("id", response.id() != null ? response.id() : "chatcmpl-langchain4j");
        result.put("object", "chat.completion");
        result.put("created", System.currentTimeMillis() / 1000);
        result.put(AiKeys.MODEL, response.modelName() != null ? response.modelName() : "unknown");
        result.put("choices", choices);
        result.put("usage", usage);
        result.put("system_fingerprint", JSONObject.NULL);
        return result.toString();
    }

    static String toEmbeddingResponseJson(final Embedding embedding) {
        final JSONArray embeddingArray = new JSONArray();
        for (final float value : embedding.vector()) {
            embeddingArray.put((double) value);
        }

        final JSONObject data = new JSONObject();
        data.put(AiKeys.EMBEDDING, embeddingArray);
        data.put(AiKeys.INDEX, 0);
        data.put("object", "embedding");

        final JSONArray dataArray = new JSONArray();
        dataArray.put(data);

        final JSONObject result = new JSONObject();
        result.put(AiKeys.DATA, dataArray);
        return result.toString();
    }

    static String toImageResponseJson(final Image image) {
        final JSONObject data = new JSONObject();
        if (image != null && image.url() != null) {
            data.put(AiKeys.URL, image.url().toString());
        } else if (image != null && image.base64Data() != null) {
            data.put(AiKeys.URL, "");
            data.put(AiKeys.B64_JSON, image.base64Data());
        } else {
            data.put(AiKeys.URL, "");
        }

        final JSONArray dataArray = new JSONArray();
        dataArray.put(data);

        final JSONObject result = new JSONObject();
        result.put(AiKeys.DATA, dataArray);
        return result.toString();
    }

    private static List<String> effectiveModels(final ProviderConfig config) {
        final List<String> models = config.allModels();
        if (!models.isEmpty()) {
            return models;
        }
        final String dep = config.deploymentName();
        return (dep != null && !dep.isBlank()) ? List.of(dep) : models;
    }

    private static ProviderConfig parseSection(final String providerConfigJson, final String section) {
        if (providerConfigJson == null) {
            throw new IllegalArgumentException("providerConfig is null — app config is not enabled");
        }
        try {
            final JsonNode root = MAPPER.readTree(providerConfigJson);
            final JsonNode sectionNode = root.get(section);
            if (sectionNode == null) {
                throw new IllegalArgumentException(
                        "Missing '" + section + "' section in providerConfig");
            }
            return MAPPER.treeToValue(sectionNode, ProviderConfig.class);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse '" + section + "' from providerConfig: " + e.getMessage(), e);
        }
    }

}
