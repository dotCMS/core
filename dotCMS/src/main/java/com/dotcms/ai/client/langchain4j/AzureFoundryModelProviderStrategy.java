package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialImageModel;

import java.time.Duration;

class AzureFoundryModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "azure_foundry";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "azure_foundry only supports image models; use azure_openai for chat and embeddings");
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "azure_foundry only supports image models; use azure_openai for chat and embeddings");
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "azure_foundry only supports image models; use azure_openai for chat and embeddings");
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiOfficialImageModel.Builder builder = OpenAiOfficialImageModel.builder()
                .baseUrl(config.endpoint())
                .apiKey(config.apiKey())
                .modelName(deploymentName(config));
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
                    "providerConfig." + modelType + ": either 'model' or 'deploymentName' is required for azure_foundry");
        }
    }

    private static String deploymentName(final ProviderConfig config) {
        return config.deploymentName() != null && !config.deploymentName().isBlank()
                ? config.deploymentName()
                : config.model();
    }

}
