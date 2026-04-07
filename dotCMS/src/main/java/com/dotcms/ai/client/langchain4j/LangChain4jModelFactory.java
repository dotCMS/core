package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters;
import dev.langchain4j.model.bedrock.BedrockCohereEmbeddingModel;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

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
 * <p>Supported providers: {@code openai}, {@code azure_openai}, {@code bedrock}
 * <p>Planned: {@code vertex_ai}
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
                LangChain4jModelFactory::buildAzureOpenAiChatModel,
                LangChain4jModelFactory::buildBedrockChatModel);
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
                LangChain4jModelFactory::buildAzureOpenAiEmbeddingModel,
                LangChain4jModelFactory::buildBedrockEmbeddingModel);
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
                LangChain4jModelFactory::buildAzureOpenAiImageModel,
                LangChain4jModelFactory::buildBedrockImageModel);
    }

    private static <T> T build(final ProviderConfig config,
                                final String modelType,
                                final Function<ProviderConfig, T> openAiFn,
                                final Function<ProviderConfig, T> azureOpenAiFn,
                                final Function<ProviderConfig, T> bedrockFn) {
        if (config == null || config.provider() == null) {
            throw new IllegalArgumentException("ProviderConfig or provider name is null for model type: " + modelType);
        }
        switch (config.provider().toLowerCase()) {
            case "openai":
                return openAiFn.apply(config);
            case "azure_openai":
                return azureOpenAiFn.apply(config);
            case "bedrock":
                return bedrockFn.apply(config);
            default:
                throw new IllegalArgumentException("Unsupported " + modelType + " provider: "
                        + config.provider() + ". Supported: openai, azure_openai, bedrock");
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
        if (config.temperature() != null) {
            builder.temperature(config.temperature());
        }
        if (config.maxCompletionTokens() != null) {
            builder.maxCompletionTokens(config.maxCompletionTokens());
        } else if (config.maxTokens() != null) {
            builder.maxTokens(config.maxTokens());
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

    private static ChatModel buildAzureOpenAiChatModel(final ProviderConfig config) {
        final AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    private static EmbeddingModel buildAzureOpenAiEmbeddingModel(final ProviderConfig config) {
        final AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    private static ImageModel buildAzureOpenAiImageModel(final ProviderConfig config) {
        final AzureOpenAiImageModel.Builder builder = AzureOpenAiImageModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(config.deploymentName() != null ? config.deploymentName() : config.model());
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.size() != null) builder.size(config.size());
        return builder.build();
    }

    // ── Bedrock builders ──────────────────────────────────────────────────────

    private static AwsCredentialsProvider bedrockCredentials(final ProviderConfig config) {
        if (config.accessKeyId() != null && config.secretAccessKey() != null) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKeyId(), config.secretAccessKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    private static BedrockRuntimeClient bedrockClient(final ProviderConfig config) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(bedrockCredentials(config))
                .build();
    }

    private static ChatModel buildBedrockChatModel(final ProviderConfig config) {
        final BedrockChatModel.Builder builder = BedrockChatModel.builder()
                .modelId(config.model())
                .region(Region.of(config.region()))
                .client(bedrockClient(config));
        if (config.temperature() != null || config.maxTokens() != null) {
            final BedrockChatRequestParameters.Builder params = BedrockChatRequestParameters.builder();
            if (config.temperature() != null) params.temperature(config.temperature());
            if (config.maxTokens() != null) params.maxOutputTokens(config.maxTokens());
            builder.defaultRequestParameters(params.build());
        }
        return builder.build();
    }

    private static EmbeddingModel buildBedrockEmbeddingModel(final ProviderConfig config) {
        final String model = config.model();
        final AwsCredentialsProvider credentials = bedrockCredentials(config);
        final Region region = Region.of(config.region());
        if (model != null && model.startsWith("cohere.")) {
            return BedrockCohereEmbeddingModel.builder()
                    .model(model)
                    .region(region)
                    .credentialsProvider(credentials)
                    .inputType("search_document")
                    .build();
        }
        return BedrockTitanEmbeddingModel.builder()
                .model(model)
                .region(region)
                .credentialsProvider(credentials)
                .build();
    }

    private static ImageModel buildBedrockImageModel(final ProviderConfig config) {
        throw new UnsupportedOperationException(
                "Image generation is not supported for Bedrock provider via LangChain4J");
    }

}
