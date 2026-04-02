package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * @return a configured {@link ChatModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static ChatModel buildChatModel(final ProviderConfig config) {
        return build(config, "chat", LangChain4jModelFactory::buildOpenAiChatModel);
    }

    /**
     * Builds an {@link EmbeddingModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the embeddings section
     * @return a configured {@link EmbeddingModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        return build(config, "embeddings", LangChain4jModelFactory::buildOpenAiEmbeddingModel);
    }

    /**
     * Builds an {@link ImageModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the image section
     * @return a configured {@link ImageModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static ImageModel buildImageModel(final ProviderConfig config) {
        return build(config, "image", LangChain4jModelFactory::buildOpenAiImageModel);
    }

    private static <T> T build(final ProviderConfig config,
                                final String modelType,
                                final Function<ProviderConfig, T> openAiFn) {
        if (config == null || config.provider() == null) {
            throw new IllegalArgumentException("ProviderConfig or provider name is null for model type: " + modelType);
        }
        switch (config.provider().toLowerCase()) {
            case "openai":
                return openAiFn.apply(config);
            default:
                throw new IllegalArgumentException("Unsupported " + modelType + " provider: "
                        + config.provider() + ". Supported in Phase 1: openai");
        }
    }

    // ── OpenAI builders ───────────────────────────────────────────────────────

    private static void applyCommonConfig(final ProviderConfig config,
                                          final Consumer<String> baseUrlFn,
                                          final Consumer<Integer> retriesFn,
                                          final Consumer<Duration> timeoutFn) {
        if (config.endpoint() != null) baseUrlFn.accept(config.endpoint());
        if (config.maxRetries() != null) retriesFn.accept(config.maxRetries());
        if (config.timeout() != null) timeoutFn.accept(Duration.ofSeconds(config.timeout()));
    }

    private static ChatModel buildOpenAiChatModel(final ProviderConfig config) {
        final OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.maxCompletionTokens() != null) {
            builder.maxCompletionTokens(config.maxCompletionTokens());
        } else if (config.maxTokens() != null) {
            builder.maxTokens(config.maxTokens());
        }
        if (config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        return builder.build();
    }

    private static EmbeddingModel buildOpenAiEmbeddingModel(final ProviderConfig config) {
        final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        return builder.build();
    }

    private static ImageModel buildOpenAiImageModel(final ProviderConfig config) {
        final OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.size() != null) {
            builder.size(config.size());
        }
        return builder.build();
    }

}
