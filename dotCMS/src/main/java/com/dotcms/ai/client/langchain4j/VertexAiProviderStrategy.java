package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiStreamingChatModel;

/**
 * {@link ProviderStrategy} for Google Vertex AI (Gemini models).
 *
 * <p>Authentication is handled via Application Default Credentials (ADC).
 * No API key is required — credentials are resolved automatically from the environment
 * (GCP instance metadata, {@code GOOGLE_APPLICATION_CREDENTIALS} env var, or gcloud CLI).
 *
 * <p>Embeddings and image generation are not supported via LangChain4J for this provider.
 */
public class VertexAiProviderStrategy implements ProviderStrategy {

    @Override
    public String providerName() {
        return "vertex_ai";
    }

    @Override
    public void validate(final ProviderConfig config, final String modelType) {
        ProviderStrategy.requireNonBlank(config.projectId(), "projectId", modelType);
        ProviderStrategy.requireNonBlank(config.location(), "location", modelType);
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config) {
        final VertexAiGeminiChatModel.VertexAiGeminiChatModelBuilder builder =
                VertexAiGeminiChatModel.builder()
                        .project(config.projectId())
                        .location(config.location())
                        .modelName(config.model());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.temperature() != null) builder.temperature(config.temperature().floatValue());
        if (config.maxTokens() != null) builder.maxOutputTokens(config.maxTokens());
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        final VertexAiGeminiStreamingChatModel.VertexAiGeminiStreamingChatModelBuilder builder =
                VertexAiGeminiStreamingChatModel.builder()
                        .project(config.projectId())
                        .location(config.location())
                        .modelName(config.model());
        if (config.temperature() != null) builder.temperature(config.temperature().floatValue());
        if (config.maxTokens() != null) builder.maxOutputTokens(config.maxTokens());
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        throw new UnsupportedOperationException(
                "Embeddings are not supported for Vertex AI provider via LangChain4J");
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config) {
        throw new UnsupportedOperationException(
                "Image generation is not supported for Vertex AI provider via LangChain4J");
    }

}
