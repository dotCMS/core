package com.dotcms.ai.api.provider.bedrock;

import com.dotcms.ai.api.provider.ChatModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Duration;

/**
 * Amazon Bedrock ChatModel Provider Implementation
 * Supports Chat models via AWS Bedrock.
 * @author jsanca
 */
public class BedrockChatModelProviderImpl implements ChatModelProvider {

    @Override
    public ChatModel create(final AiModelConfig config) {

        // For authentication, set the following environment variables:
        // AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
        // More info on creating the API keys:
        // https://docs.aws.amazon.com/bedrock/latest/userguide/api-setup.html
        final String accessKeyId      = config.getOrDefault("accessKeyId", "");
        final String secretAccessKey  = config.getOrDefault("secretAccessKey", "");
        final String region      = config.getOrDefault("region", "");
        final String modelId     = config.getOrDefault(AiModelConfig.MODEL, "");
        final double temperature = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"),
                0.3);
        final long timeoutMs     = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"),
                30000L);

        final AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        final AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        final BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();

        return BedrockChatModel.builder()
                //.awsApiKey(apiKey)
                .client(bedrockClient)
                .modelId(modelId)
                //.temperature(temperature)  // todo: need to research may be defaultRequestParameters
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {

        // For authentication, set the following environment variables:
        // AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
        // More info on creating the API keys:
        // https://docs.aws.amazon.com/bedrock/latest/userguide/api-setup.html
        final String accessKeyId      = config.getOrDefault("accessKeyId", "");
        final String secretAccessKey  = config.getOrDefault("secretAccessKey", "");
        final String region      = config.getOrDefault("region", "");
        final String modelId     = config.getOrDefault(AiModelConfig.MODEL, "");
        final double temperature = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"),
                0.3);
        final long timeoutMs     = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"),
                30000L);
        final AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        final AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        final BedrockRuntimeAsyncClient bedrockClient = BedrockRuntimeAsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();

        return BedrockStreamingChatModel.builder()
                //.awsApiKey(apiKey)
                .client(bedrockClient)
                .modelId(modelId)
                //.temperature(temperature)  // todo: need to research may be defaultRequestParameters
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
