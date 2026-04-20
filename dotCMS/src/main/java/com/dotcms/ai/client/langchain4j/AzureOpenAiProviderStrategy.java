package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiImageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.time.Duration;

/**
 * {@link ProviderStrategy} for Azure OpenAI Service.
 */
public class AzureOpenAiProviderStrategy implements ProviderStrategy {

    @Override
    public String providerName() {
        return "azure_openai";
    }

    @Override
    public void validate(final ProviderConfig config, final String modelType) {
        ProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
        ProviderStrategy.requireNonBlank(config.endpoint(), "endpoint", modelType);
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config) {
        final AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        final AzureOpenAiStreamingChatModel.Builder builder = AzureOpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        final AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config) {
        final AzureOpenAiImageModel.Builder builder = AzureOpenAiImageModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.size() != null) builder.size(config.size());
        return builder.build();
    }

    private static String deploymentName(final ProviderConfig config) {
        return config.deploymentName() != null ? config.deploymentName() : config.model();
    }

}
