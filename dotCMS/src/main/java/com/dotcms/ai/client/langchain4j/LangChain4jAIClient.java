package com.dotcms.ai.client.langchain4j;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
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
        final List<String> models = effectiveModels(baseConfig);
        if (models.isEmpty()) {
            throw new IllegalArgumentException(
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
                final String result = executor.apply(model);
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
