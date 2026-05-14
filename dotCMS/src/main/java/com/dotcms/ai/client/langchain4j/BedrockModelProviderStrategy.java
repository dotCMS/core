package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters;
import dev.langchain4j.model.bedrock.BedrockCohereEmbeddingModel;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

class BedrockModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "bedrock";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        warnIgnoredOptions(config);
        final BedrockChatModel.Builder builder = BedrockChatModel.builder()
                .modelId(config.model())
                .client(bedrockClient(config));
        if (config.temperature() != null || config.maxTokens() != null) {
            final BedrockChatRequestParameters.Builder params = BedrockChatRequestParameters.builder();
            if (config.temperature() != null) params.temperature(config.temperature());
            if (config.maxTokens() != null) params.maxOutputTokens(config.maxTokens());
            builder.defaultRequestParameters(params.build());
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        warnIgnoredOptions(config);
        final BedrockStreamingChatModel.Builder builder = BedrockStreamingChatModel.builder()
                .modelId(config.model())
                .client(bedrockAsyncClient(config));
        if (config.temperature() != null || config.maxTokens() != null) {
            final BedrockChatRequestParameters.Builder params = BedrockChatRequestParameters.builder();
            if (config.temperature() != null) params.temperature(config.temperature());
            if (config.maxTokens() != null) params.maxOutputTokens(config.maxTokens());
            builder.defaultRequestParameters(params.build());
        }
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        warnIgnoredOptions(config);
        final String model = config.model();
        final AwsCredentialsProvider credentials = credentials(config);
        final Region region = Region.of(config.region());
        final String modelLower = model.toLowerCase();
        if (modelLower.startsWith("cohere.")) {
            return BedrockCohereEmbeddingModel.builder()
                    .model(model)
                    .region(region)
                    .credentialsProvider(credentials)
                    .inputType(config.embeddingInputType())
                    .build();
        }
        if (modelLower.startsWith("amazon.titan-")) {
            return BedrockTitanEmbeddingModel.builder()
                    .model(model)
                    .region(region)
                    .credentialsProvider(credentials)
                    .build();
        }
        throw new IllegalArgumentException(
                "Unsupported Bedrock embedding model: '" + model + "'. Supported families: cohere.*, amazon.titan-*");
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "Image generation is not supported for Bedrock provider via LangChain4J");
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.region(), "region", modelType);
        ModelProviderStrategy.requireNonBlank(config.model(), "model", modelType);
    }

    private static void warnIgnoredOptions(final ProviderConfig config) {
        if (config.timeout() != null || config.maxRetries() != null) {
            Logger.warn(BedrockModelProviderStrategy.class,
                    "timeout and maxRetries are not applied to Bedrock providers and will be ignored");
        }
    }

    private static AwsCredentialsProvider credentials(final ProviderConfig config) {
        final boolean hasKeyId = config.accessKeyId() != null && !config.accessKeyId().isBlank();
        final boolean hasSecret = config.secretAccessKey() != null && !config.secretAccessKey().isBlank();
        if (hasKeyId && hasSecret) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKeyId(), config.secretAccessKey()));
        }
        if (hasKeyId || hasSecret) {
            throw new IllegalArgumentException(
                    "Bedrock: 'accessKeyId' and 'secretAccessKey' must both be set or both be absent");
        }
        return DefaultCredentialsProvider.create();
    }

    private static BedrockRuntimeClient bedrockClient(final ProviderConfig config) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(credentials(config))
                .build();
    }

    private static BedrockRuntimeAsyncClient bedrockAsyncClient(final ProviderConfig config) {
        return BedrockRuntimeAsyncClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(credentials(config))
                .build();
    }

}
