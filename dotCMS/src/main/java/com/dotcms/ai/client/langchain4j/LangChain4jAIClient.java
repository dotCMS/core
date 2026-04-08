package com.dotcms.ai.client.langchain4j;

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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import io.vavr.Lazy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    private final Cache<String, ChatModel> chatModelCache = CacheBuilder.newBuilder()
            .expireAfterWrite(MODEL_CACHE_TTL_HOURS, TimeUnit.HOURS)
            .build();
    private final Cache<String, EmbeddingModel> embeddingModelCache = CacheBuilder.newBuilder()
            .expireAfterWrite(MODEL_CACHE_TTL_HOURS, TimeUnit.HOURS)
            .build();
    private final Cache<String, ImageModel> imageModelCache = CacheBuilder.newBuilder()
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

        final String cacheKeyPrefix = appConfig.getHost() + ":" + sha256Hex(providerConfigJson);

        final String responseJson;
        if (type == AIModelType.IMAGE) {
            responseJson = executeImageRequest(cacheKeyPrefix, providerConfigJson, payload);
        } else if (type == AIModelType.EMBEDDINGS) {
            responseJson = executeEmbeddingRequest(cacheKeyPrefix, providerConfigJson, payload);
        } else {
            responseJson = executeChatRequest(cacheKeyPrefix, providerConfigJson, payload);
        }

        try {
            output.write(responseJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(this, "Failed to write AI response to output stream: " + e.getMessage(), e);
            throw new DotAIClientConnectException("Failed to write AI response to output stream: " + e.getMessage(), e);
        }
    }

    private String executeChatRequest(final String cacheKeyPrefix, final String providerConfigJson, final JSONObject payload) {
        final ChatModel model;
        try {
            model = chatModelCache.get(
                    cacheKeyPrefix + ":chat",
                    () -> LangChain4jModelFactory.buildChatModel(parseSection(providerConfigJson, "chat")));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException("Failed to initialize chat model: " + e.getCause().getMessage(), e.getCause());
        }

        final List<ChatMessage> messages = toMessages(payload.optJSONArray(AiKeys.MESSAGES));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("Chat request must contain at least one message");
        }

        final ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);

        final Object temperature = payload.opt(AiKeys.TEMPERATURE);
        if (temperature instanceof Number) {
            requestBuilder.temperature(((Number) temperature).doubleValue());
        }
        final Object maxTokens = payload.opt(AiKeys.MAX_TOKENS);
        if (maxTokens instanceof Number) {
            requestBuilder.maxOutputTokens(((Number) maxTokens).intValue());
        }

        final ChatResponse response = model.chat(requestBuilder.build());
        return toChatResponseJson(response);
    }

    private String executeEmbeddingRequest(final String cacheKeyPrefix, final String providerConfigJson, final JSONObject payload) {
        final EmbeddingModel model;
        try {
            model = embeddingModelCache.get(
                    cacheKeyPrefix + ":embeddings",
                    () -> LangChain4jModelFactory.buildEmbeddingModel(parseSection(providerConfigJson, "embeddings")));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException("Failed to initialize embedding model: " + e.getCause().getMessage(), e.getCause());
        }

        final String input = payload.getString(AiKeys.INPUT);
        final Response<Embedding> response = model.embed(TextSegment.from(input));
        return toEmbeddingResponseJson(response.content());
    }

    private String executeImageRequest(final String cacheKeyPrefix, final String providerConfigJson, final JSONObject payload) {
        final ImageModel model;
        try {
            model = imageModelCache.get(
                    cacheKeyPrefix + ":image",
                    () -> LangChain4jModelFactory.buildImageModel(parseSection(providerConfigJson, "image")));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException("Failed to initialize image model: " + e.getCause().getMessage(), e.getCause());
        }

        final String prompt = payload.getString(AiKeys.PROMPT);
        final Response<Image> response = model.generate(prompt);
        return toImageResponseJson(response.content());
    }

    static List<ChatMessage> toMessages(final JSONArray messagesArray) {
        final List<ChatMessage> messages = new ArrayList<>();
        if (messagesArray == null) {
            return messages;
        }
        for (int i = 0; i < messagesArray.length(); i++) {
            final JSONObject msg = messagesArray.getJSONObject(i);
            final String role = msg.optString(AiKeys.ROLE, AiKeys.USER).toLowerCase();
            final String content = msg.optString(AiKeys.CONTENT, "");
            if ("system".equals(role)) {
                messages.add(new SystemMessage(content));
            } else if ("assistant".equals(role)) {
                messages.add(new AiMessage(content));
            } else {
                messages.add(new UserMessage(content));
            }
        }
        return messages;
    }

    static String toChatResponseJson(final ChatResponse response) {
        final JSONObject message = new JSONObject();
        message.put(AiKeys.ROLE, "assistant");
        message.put(AiKeys.CONTENT, response.aiMessage().text());

        final JSONObject choice = new JSONObject();
        choice.put(AiKeys.MESSAGE, message);
        choice.put(AiKeys.INDEX, 0);
        choice.put("finish_reason", "stop");

        final JSONArray choices = new JSONArray();
        choices.put(choice);

        final JSONObject result = new JSONObject();
        result.put("choices", choices);
        result.put(AiKeys.MODEL, response.modelName());
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

    private static String sha256Hex(final String input) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static ProviderConfig parseSection(final String providerConfigJson, final String section) {
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
