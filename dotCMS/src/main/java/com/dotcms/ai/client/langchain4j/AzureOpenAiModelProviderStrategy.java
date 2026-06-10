package com.dotcms.ai.client.langchain4j;

import com.openai.azure.AzureOpenAIServiceVersion;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialImageModel;

import java.time.Duration;

class AzureOpenAiModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "azure_openai";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null && !requiresCompletionTokens(config)) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AzureOpenAiStreamingChatModel.Builder builder = AzureOpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null && !requiresCompletionTokens(config)) builder.maxTokens(config.maxTokens());
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) builder.serviceVersion(config.apiVersion());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.dimensions() != null) builder.dimensions(config.dimensions());
        return builder.build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        if (config.endpoint().contains("services.ai.azure.com")) {
            final OpenAiOfficialImageModel.Builder builder = OpenAiOfficialImageModel.builder()
                    .baseUrl(config.endpoint())
                    .apiKey(config.apiKey())
                    .modelName(deploymentName(config));
            if (config.size() != null) builder.size(config.size());
            if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
            if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
            return builder.build();
        }
        final OpenAiOfficialImageModel.Builder builder = OpenAiOfficialImageModel.builder()
                .isMicrosoftFoundry(true)
                .baseUrl(config.endpoint())
                .apiKey(config.apiKey())
                .microsoftFoundryDeploymentName(deploymentName(config))
                .modelName(deploymentName(config));
        if (config.apiVersion() != null) {
            builder.azureOpenAIServiceVersion(AzureOpenAIServiceVersion.Companion.fromString(config.apiVersion()));
        }
        if (config.size() != null) builder.size(config.size());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        return builder.build();
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
        ModelProviderStrategy.requireNonBlank(config.endpoint(), "endpoint", modelType);
        if ((config.model() == null || config.model().isBlank())
                && (config.deploymentName() == null || config.deploymentName().isBlank())) {
            throw new IllegalArgumentException(
                    "providerConfig." + modelType + ": either 'model' or 'deploymentName' is required for azure_openai");
        }
    }

    private static String deploymentName(final ProviderConfig config) {
        return config.deploymentName() != null && !config.deploymentName().isBlank()
                ? config.deploymentName()
                : config.model();
    }

    private static boolean requiresCompletionTokens(final ProviderConfig config) {
        final String name = deploymentName(config) != null ? deploymentName(config) : "";
        return name.matches("o\\d+.*") || name.matches("gpt-([5-9]|\\d{2,}).*");
    }

}
