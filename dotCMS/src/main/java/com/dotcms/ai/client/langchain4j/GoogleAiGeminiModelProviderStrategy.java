package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiImageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.image.ImageModel;

import java.time.Duration;

/**
 * {@link ModelProviderStrategy} implementation for Google AI (Gemini API / AI Studio).
 *
 * <p>This is the consumer-facing Gemini API authenticated with an API key from
 * <a href="https://aistudio.google.com">Google AI Studio</a> — distinct from the
 * {@code vertex_ai} provider, which targets the same model family through Google
 * Cloud (project/location + service-account or ADC auth).
 *
 * <p>Supports chat (streaming and non-streaming), embeddings
 * (e.g. {@code gemini-embedding-001}, {@code text-embedding-004}) and image
 * generation (e.g. {@code gemini-2.5-flash-image}).
 */
class GoogleAiGeminiModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "google_ai";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder builder = GoogleAiGeminiChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        if (config.endpoint() != null) builder.baseUrl(config.endpoint());
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxOutputTokens(config.maxTokens());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final GoogleAiGeminiStreamingChatModel.GoogleAiGeminiStreamingChatModelBuilder builder =
                GoogleAiGeminiStreamingChatModel.builder()
                        .apiKey(config.apiKey())
                        .modelName(config.model());
        if (config.maxRetries() != null) {
            Logger.warn(GoogleAiGeminiModelProviderStrategy.class,
                    "maxRetries is not supported by the Google AI Gemini streaming chat model and will be ignored");
        }
        if (config.endpoint() != null) builder.baseUrl(config.endpoint());
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxOutputTokens(config.maxTokens());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final GoogleAiEmbeddingModel.GoogleAiEmbeddingModelBuilder builder = GoogleAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        if (config.endpoint() != null) builder.baseUrl(config.endpoint());
        if (config.dimensions() != null) builder.outputDimensionality(config.dimensions());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final GoogleAiGeminiImageModel.GoogleAiGeminiImageModelBuilder builder = GoogleAiGeminiImageModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        if (config.endpoint() != null) builder.baseUrl(config.endpoint());
        if (config.size() != null) builder.imageSize(config.size());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.model(), "model", modelType);
        ModelProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
    }

}
