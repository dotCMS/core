package com.dotcms.ai.client.langchain4j;

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

/**
 * {@link ProviderStrategy} for AWS Bedrock.
 *
 * <p>Credentials are resolved in order:
 * <ol>
 *   <li>Explicit {@code accessKeyId} + {@code secretAccessKey} in {@link ProviderConfig}</li>
 *   <li>{@link DefaultCredentialsProvider} — IAM role, environment variables, ~/.aws/credentials</li>
 * </ol>
 *
 * <p>Image generation is not supported via LangChain4J for this provider.
 */
public class BedrockProviderStrategy implements ProviderStrategy {

    @Override
    public String providerName() {
        return "bedrock";
    }

    @Override
    public void validate(final ProviderConfig config, final String modelType) {
        ProviderStrategy.requireNonBlank(config.region(), "region", modelType);
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config) {
        final BedrockChatModel.Builder builder = BedrockChatModel.builder()
                .modelId(config.model())
                .region(Region.of(config.region()))
                .client(buildSyncClient(config));
        if (config.temperature() != null || config.maxTokens() != null) {
            final BedrockChatRequestParameters.Builder params = BedrockChatRequestParameters.builder();
            if (config.temperature() != null) params.temperature(config.temperature());
            if (config.maxTokens() != null) params.maxOutputTokens(config.maxTokens());
            builder.defaultRequestParameters(params.build());
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        return BedrockStreamingChatModel.builder()
                .modelId(config.model())
                .client(buildAsyncClient(config))
                .build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        final AwsCredentialsProvider credentials = resolveCredentials(config);
        final Region region = Region.of(config.region());
        if (config.model() != null && config.model().startsWith("cohere.")) {
            return BedrockCohereEmbeddingModel.builder()
                    .model(config.model())
                    .region(region)
                    .credentialsProvider(credentials)
                    .inputType(config.embeddingInputType())
                    .build();
        }
        return BedrockTitanEmbeddingModel.builder()
                .model(config.model())
                .region(region)
                .credentialsProvider(credentials)
                .build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config) {
        throw new UnsupportedOperationException(
                "Image generation is not supported for Bedrock provider via LangChain4J");
    }

    private static AwsCredentialsProvider resolveCredentials(final ProviderConfig config) {
        if (config.accessKeyId() != null && config.secretAccessKey() != null) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKeyId(), config.secretAccessKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    private static BedrockRuntimeClient buildSyncClient(final ProviderConfig config) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(resolveCredentials(config))
                .build();
    }

    private static BedrockRuntimeAsyncClient buildAsyncClient(final ProviderConfig config) {
        return BedrockRuntimeAsyncClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(resolveCredentials(config))
                .build();
    }

}
