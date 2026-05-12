package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

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
 * <p>Supported providers: {@code openai}, {@code azure_openai}
 * <p>Planned (Phase 2): {@code bedrock}, {@code vertex_ai}
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
        return build(config, "chat",
                LangChain4jModelFactory::buildOpenAiChatModel,
                LangChain4jModelFactory::buildAzureOpenAiChatModel);
    }

    /**
     * Builds a {@link StreamingChatModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the chat section
     * @return a configured {@link StreamingChatModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        return build(config, "chat",
                LangChain4jModelFactory::buildOpenAiStreamingChatModel,
                LangChain4jModelFactory::buildAzureOpenAiStreamingChatModel);
    }

    /**
     * Builds an {@link EmbeddingModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the embeddings section
     * @return a configured {@link EmbeddingModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        return build(config, "embeddings",
                LangChain4jModelFactory::buildOpenAiEmbeddingModel,
                LangChain4jModelFactory::buildAzureOpenAiEmbeddingModel);
    }

    /**
     * Builds an {@link ImageModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the image section
     * @return a configured {@link ImageModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static ImageModel buildImageModel(final ProviderConfig config) {
        return build(config, "image",
                LangChain4jModelFactory::buildOpenAiImageModel,
                LangChain4jModelFactory::buildAzureOpenAiImageModel);
    }

    private static <T> T build(final ProviderConfig config,
                                final String modelType,
                                final Function<ProviderConfig, T> openAiFn,
                                final Function<ProviderConfig, T> azureOpenAiFn) {
        if (config == null || config.provider() == null) {
            throw new IllegalArgumentException("ProviderConfig or provider name is null for model type: " + modelType);
        }
        switch (config.provider().toLowerCase()) {
            case "openai":
                requireNonBlank(config.model(), "model", modelType);
                validateOpenAi(config, modelType);
                return openAiFn.apply(config);
            case "azure_openai":
                validateAzureOpenAi(config, modelType);
                return azureOpenAiFn.apply(config);
            default:
                throw new IllegalArgumentException("Unsupported " + modelType + " provider: "
                        + config.provider() + ". Supported: openai, azure_openai");
        }
    }

    private static void validateOpenAi(final ProviderConfig config, final String modelType) {
        requireNonBlank(config.apiKey(), "apiKey", modelType);
    }

    private static void validateAzureOpenAi(final ProviderConfig config, final String modelType) {
        requireNonBlank(config.apiKey(), "apiKey", modelType);
        requireNonBlank(config.endpoint(), "endpoint", modelType);
        if ((config.model() == null || config.model().isBlank())
                && (config.deploymentName() == null || config.deploymentName().isBlank())) {
            throw new IllegalArgumentException(
                    "providerConfig." + modelType + ": either 'model' or 'deploymentName' is required for azure_openai");
        }
    }

    private static void requireNonBlank(final String value, final String field, final String modelType) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "providerConfig." + modelType + "." + field + " is required but was not set");
        }
    }

    // ── OpenAI builders ───────────────────────────────────────────────────────

    /**
     * OpenAI reasoning models (o1, o3, o4-mini, etc.) and newer GPT models (gpt-5.x+) require
     * {@code max_completion_tokens} instead of {@code max_tokens}. Given a single user-facing
     * {@code maxTokens} field, this method routes to the correct builder parameter automatically.
     */
    private static void applyOpenAiTokenLimit(
            final ProviderConfig config,
            final Consumer<Integer> maxTokensFn,
            final Consumer<Integer> maxCompletionTokensFn) {
        final Integer tokens = config.maxCompletionTokens() != null
                ? config.maxCompletionTokens()
                : config.maxTokens();
        if (tokens == null) {
            return;
        }
        final String model = config.model() != null ? config.model() : "";
        if (model.matches("o\\d+.*") || model.matches("gpt-([5-9]|\\d{2,}).*")) {
            maxCompletionTokensFn.accept(tokens);
        } else {
            maxTokensFn.accept(tokens);
        }
    }

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
        if (config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        applyOpenAiTokenLimit(config, builder::maxTokens, builder::maxCompletionTokens);
        return builder.build();
    }

    private static StreamingChatModel buildOpenAiStreamingChatModel(final ProviderConfig config) {
        final OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl,
                ignored -> Logger.warn(LangChain4jModelFactory.class,
                        "maxRetries is not supported by OpenAiStreamingChatModel and will be ignored"),
                builder::timeout);
        if (config.temperature() != null) builder.temperature(config.temperature());
        applyOpenAiTokenLimit(config, builder::maxTokens, builder::maxCompletionTokens);
        return builder.build();
    }

    private static EmbeddingModel buildOpenAiEmbeddingModel(final ProviderConfig config) {
        final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.dimensions() != null) {
            builder.dimensions(config.dimensions());
        }
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

    // ── Azure OpenAI builders ─────────────────────────────────────────────────

    private static boolean requiresCompletionTokens(final ProviderConfig config) {
        final String name = config.deploymentName() != null && !config.deploymentName().isBlank()
                ? config.deploymentName()
                : (config.model() != null ? config.model() : "");
        return name.matches("o\\d+.*") || name.matches("gpt-([5-9]|\\d{2,}).*");
    }

    private static StreamingChatModel buildAzureOpenAiStreamingChatModel(final ProviderConfig config) {
        final AzureOpenAiStreamingChatModel.Builder builder = AzureOpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null && !config.deploymentName().isBlank() ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null && !requiresCompletionTokens(config)) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    private static ChatModel buildAzureOpenAiChatModel(final ProviderConfig config) {
        final AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null && !config.deploymentName().isBlank() ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null && !requiresCompletionTokens(config)) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    private static EmbeddingModel buildAzureOpenAiEmbeddingModel(final ProviderConfig config) {
        final AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null && !config.deploymentName().isBlank() ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.dimensions() != null) builder.dimensions(config.dimensions());
        return builder.build();
    }

    private static ImageModel buildAzureOpenAiImageModel(final ProviderConfig config) {
        final AzureOpenAiImageModel.Builder builder = AzureOpenAiImageModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null && !config.deploymentName().isBlank() ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.size() != null) builder.size(config.size());
        return builder.build();
    }

}
