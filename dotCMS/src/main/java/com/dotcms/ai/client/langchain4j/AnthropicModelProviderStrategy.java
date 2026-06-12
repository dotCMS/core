package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.time.Duration;

/**
 * {@link ModelProviderStrategy} implementation for Anthropic (Claude).
 *
 * <p>Talks directly to the Anthropic Messages API with an API key — distinct from
 * accessing Claude models through AWS Bedrock. The {@code endpoint} config field
 * overrides the default base URL if set (useful for proxies/gateways).
 *
 * <p>Model IDs use the Anthropic form, e.g. {@code claude-sonnet-4-6},
 * {@code claude-opus-4-8}, {@code claude-haiku-4-5}.
 *
 * <p>Supports chat (streaming and non-streaming) only. Anthropic does not offer
 * embeddings or image-generation APIs.
 */
class AnthropicModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "anthropic";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        if (config.endpoint() != null) builder.baseUrl(config.endpoint());
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxTokens(config.maxTokens());
        if (config.maxRetries() != null) builder.maxRetries(config.maxRetries());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder builder =
                AnthropicStreamingChatModel.builder()
                        .apiKey(config.apiKey())
                        .modelName(config.model());
        if (config.maxRetries() != null) {
            Logger.warn(AnthropicModelProviderStrategy.class,
                    "maxRetries is not supported by the Anthropic streaming chat model and will be ignored");
        }
        if (config.endpoint() != null) builder.baseUrl(config.endpoint());
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxTokens() != null) builder.maxTokens(config.maxTokens());
        if (config.timeout() != null) builder.timeout(Duration.ofSeconds(config.timeout()));
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "Embeddings are not supported by Anthropic (no embeddings API)");
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "Image generation is not supported by Anthropic (no image API)");
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.model(), "model", modelType);
        ModelProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
    }

}
