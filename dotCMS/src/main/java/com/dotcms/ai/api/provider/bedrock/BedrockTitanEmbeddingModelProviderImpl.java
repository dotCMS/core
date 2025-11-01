package com.dotcms.ai.api.provider.bedrock;

import com.dotcms.ai.api.provider.EmbeddingModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Amazon Bedrock EmbeddingModel Provider Implementation
 * Supports embedding models via AWS Bedrock.
 */
public class BedrockTitanEmbeddingModelProviderImpl implements EmbeddingModelProvider {

    private static final int DEFAULT_DIMENSION = 1024;
    private static final String DEFAULT_DIMENSION_AS_STRING = String.valueOf(DEFAULT_DIMENSION);

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {

        // For authentication, set the following environment variables:
        // AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
        // More info on creating the API keys:
        // https://docs.aws.amazon.com/bedrock/latest/userguide/api-setup.html
        final String accessKeyId      = config.getOrDefault("accessKeyId", "");
        final String secretAccessKey  = config.getOrDefault("secretAccessKey", "");
        final String region   = config.getOrDefault("region", "");
        final String modelId  = config.getOrDefault(AiModelConfig.MODEL, "");
        final int dimensions = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.DIMENSIONS, DEFAULT_DIMENSION_AS_STRING), DEFAULT_DIMENSION);
        final AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        final AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        // todo: note: by now it seems to be the one we do want to use, but there are more some spike may be needed here
        return BedrockTitanEmbeddingModel.builder()
                //.awsApiKey(apiKey)
                .region(Region.of(region))
                .dimensions(dimensions)
                .model(modelId)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
