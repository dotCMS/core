package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

import java.time.Duration;

/**
 * Factory for creating LangChain4J model instances from a {@link ProviderConfig}.
 *
 * <p>This is the <strong>only class</strong> that contains provider-specific builder logic.
 * To add support for a new provider, add a case to each switch block below.
 * No other class needs to change.
 *
 * <p>Supported providers (Phase 1): {@code openai}
 * <p>Planned (Phase 2): {@code azure_openai}, {@code bedrock}, {@code vertex_ai}
 */
public class LangChain4jModelFactory {

    private LangChain4jModelFactory() {}

    /**
     * Builds a {@link ChatModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the chat section
     * @return a configured {@link ChatModel}, or {@code null} if config is null
     */
    public static ChatModel buildChatModel(final ProviderConfig config) {
        if (config == null || config.getProvider() == null) {
            return null;
        }
        switch (config.getProvider().toLowerCase()) {
            case "openai":
                return buildOpenAiChatModel(config);
            default:
                throw new IllegalArgumentException("Unsupported chat provider: " + config.getProvider()
                        + ". Supported in Phase 1: openai");
        }
    }

    /**
     * Builds an {@link EmbeddingModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the embeddings section
     * @return a configured {@link EmbeddingModel}, or {@code null} if config is null
     */
    public static EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        if (config == null || config.getProvider() == null) {
            return null;
        }
        switch (config.getProvider().toLowerCase()) {
            case "openai":
                return buildOpenAiEmbeddingModel(config);
            default:
                throw new IllegalArgumentException("Unsupported embedding provider: " + config.getProvider()
                        + ". Supported in Phase 1: openai");
        }
    }

    /**
     * Builds an {@link ImageModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the image section
     * @return a configured {@link ImageModel}, or {@code null} if config is null
     */
    public static ImageModel buildImageModel(final ProviderConfig config) {
        if (config == null || config.getProvider() == null) {
            return null;
        }
        switch (config.getProvider().toLowerCase()) {
            case "openai":
                return buildOpenAiImageModel(config);
            default:
                throw new IllegalArgumentException("Unsupported image provider: " + config.getProvider()
                        + ". Supported in Phase 1: openai");
        }
    }

    // ── OpenAI builders ───────────────────────────────────────────────────────

    private static ChatModel buildOpenAiChatModel(final ProviderConfig config) {
        final OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel());
        if (config.getEndpoint() != null) {
            builder.baseUrl(config.getEndpoint());
        }
        if (config.getMaxCompletionTokens() != null) {
            builder.maxCompletionTokens(config.getMaxCompletionTokens());
        } else if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        }
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        }
        if (config.getMaxRetries() != null) {
            builder.maxRetries(config.getMaxRetries());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }
        return builder.build();
    }

    private static EmbeddingModel buildOpenAiEmbeddingModel(final ProviderConfig config) {
        final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel());
        if (config.getEndpoint() != null) {
            builder.baseUrl(config.getEndpoint());
        }
        if (config.getMaxRetries() != null) {
            builder.maxRetries(config.getMaxRetries());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }
        return builder.build();
    }

    private static ImageModel buildOpenAiImageModel(final ProviderConfig config) {
        final OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel());
        if (config.getEndpoint() != null) {
            builder.baseUrl(config.getEndpoint());
        }
        if (config.getSize() != null) {
            builder.size(config.getSize());
        }
        if (config.getMaxRetries() != null) {
            builder.maxRetries(config.getMaxRetries());
        }
        if (config.getTimeout() != null) {
            builder.timeout(Duration.ofSeconds(config.getTimeout()));
        }
        return builder.build();
    }

}
