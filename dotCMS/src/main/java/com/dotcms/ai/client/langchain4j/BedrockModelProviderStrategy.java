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
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;

import java.time.Duration;
import java.util.Optional;

/**
 * {@link ModelProviderStrategy} for Amazon Bedrock, backed by LangChain4J's Bedrock modules.
 *
 * <p><b>Configuration fields</b> (from {@link ProviderConfig}):
 * <ul>
 *   <li>{@code region} – AWS region, e.g. {@code us-east-1} (required)</li>
 *   <li>{@code model} – Bedrock model ID or inference-profile ID (required)</li>
 *   <li>{@code accessKeyId} / {@code secretAccessKey} – static credentials (optional)</li>
 *   <li>{@code temperature} / {@code maxTokens} – chat request parameters (optional)</li>
 *   <li>{@code timeout} – per-attempt request timeout in seconds, applied as {@code apiCallAttemptTimeout} (optional)</li>
 *   <li>{@code maxRetries} – retry attempts, applied to the SDK retry strategy as
 *       {@code maxAttempts = max(1, maxRetries + 1)} (optional; not applied to embedding models)</li>
 *   <li>{@code embeddingInputType} – Cohere embeddings only (optional)</li>
 * </ul>
 *
 * <p><b>Credentials.</b> {@code accessKeyId} and {@code secretAccessKey} must be supplied together
 * (both set) or both omitted — supplying only one fails fast with an {@link IllegalArgumentException}.
 * When both are absent, the AWS {@code DefaultCredentialsProvider} chain is used, which covers
 * environment variables, system properties, profile files, and the container/EKS IRSA web-identity
 * provider — the recommended path for in-cluster deployments.
 *
 * <p><b>Model ID forms.</b> Bedrock accepts two distinct forms depending on the model:
 * <ul>
 *   <li><b>Inference-profile-prefixed</b> ({@code us.}, {@code eu.}, {@code apac.}) is
 *       <em>required</em> for models offered only via cross-region inference profiles.
 *       Example: {@code us.deepseek.r1-v1:0}. The bare ID {@code deepseek.r1-v1:0} is rejected.</li>
 *   <li><b>Bare on-demand IDs</b> for models with on-demand throughput.
 *       Examples: {@code openai.gpt-oss-120b-1:0}, {@code amazon.titan-embed-text-v2:0}.
 *       These have no inference profile; do not add a region prefix.</li>
 * </ul>
 *
 * <p><b>Common Bedrock errors and their meaning:</b>
 * <ul>
 *   <li>{@code ValidationException: ... on-demand throughput isn't supported} – the model is
 *       inference-profile-only; use the profile-prefixed ID (e.g. {@code us.deepseek.r1-v1:0}).</li>
 *   <li>{@code ValidationException: This model doesn't support the stopSequences field} – a
 *       limitation of {@code langchain4j-bedrock} versions before 1.16.0, which unconditionally send
 *       {@code stopSequences} in the Converse request. It affects the gpt-oss family
 *       ({@code openai.gpt-oss-120b-1:0}, {@code openai.gpt-oss-20b-1:0}) for both chat and streaming,
 *       independent of request parameters. Resolved by upgrading langchain4j-bedrock to 1.16.0+.</li>
 *   <li>{@code AccessDeniedException} – model access has not been enabled for the account in the
 *       Bedrock model-access console.</li>
 * </ul>
 *
 * <p><b>Embeddings.</b> Supported families are {@code cohere.*}
 * ({@link BedrockCohereEmbeddingModel}, honoring {@code embeddingInputType}) and
 * {@code amazon.titan-*} ({@link BedrockTitanEmbeddingModel}); family matching is case-insensitive.
 * Any other family throws an {@link IllegalArgumentException}.
 * Note: {@code timeout} and {@code maxRetries} are not applied to embedding models — langchain4j
 * constructs the underlying Bedrock client internally and does not expose override configuration.
 *
 * <p><b>Images.</b> Image generation is not supported for Bedrock via this integration;
 * {@link #buildImageModel(ProviderConfig, String)} throws {@link UnsupportedOperationException}.
 */
class BedrockModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "bedrock";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
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
        if (config.timeout() != null) {
            Logger.warn(BedrockModelProviderStrategy.class,
                    "timeout is not supported for Bedrock embedding models and will be ignored");
        }
        if (config.maxRetries() != null) {
            Logger.warn(BedrockModelProviderStrategy.class,
                    "maxRetries is not supported for Bedrock embedding models and will be ignored");
        }
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
        final BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(credentials(config));
        overrideConfiguration(config).ifPresent(builder::overrideConfiguration);
        return builder.build();
    }

    private static BedrockRuntimeAsyncClient bedrockAsyncClient(final ProviderConfig config) {
        final BedrockRuntimeAsyncClientBuilder builder = BedrockRuntimeAsyncClient.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(credentials(config));
        overrideConfiguration(config).ifPresent(builder::overrideConfiguration);
        return builder.build();
    }

    /**
     * Builds a {@link ClientOverrideConfiguration} carrying the request timeout and/or retry
     * settings, but only when at least one option is set. Returns {@link Optional#empty()} when
     * neither {@code timeout} nor {@code maxRetries} is configured, so the AWS SDK defaults apply
     * untouched.
     *
     * <p>{@code timeout} (seconds) maps to {@link ClientOverrideConfiguration.Builder#apiCallAttemptTimeout(Duration)},
     * matching the per-request semantics of other providers (OpenAI, Azure). Each attempt gets the
     * full timeout budget; {@code maxRetries} controls how many additional attempts are made.
     * {@code maxRetries} maps to the non-deprecated retry API
     * ({@link ClientOverrideConfiguration.Builder#retryStrategy}); attempts are {@code max(1, maxRetries + 1)}
     * (one initial call plus the configured number of retries), clamped to a minimum of 1.
     */
    private static Optional<ClientOverrideConfiguration> overrideConfiguration(final ProviderConfig config) {
        if (config.timeout() == null && config.maxRetries() == null) {
            return Optional.empty();
        }
        final ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();
        if (config.timeout() != null) {
            builder.apiCallAttemptTimeout(Duration.ofSeconds(config.timeout()));
        }
        if (config.maxRetries() != null) {
            builder.retryStrategy(AwsRetryStrategy.standardRetryStrategy()
                    .toBuilder()
                    .maxAttempts(Math.max(1, config.maxRetries() + 1))
                    .build());
        }
        return Optional.of(builder.build());
    }

}
