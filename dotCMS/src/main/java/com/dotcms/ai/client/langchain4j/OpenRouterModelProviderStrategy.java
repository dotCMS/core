package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * {@link ModelProviderStrategy} implementation for OpenRouter.
 *
 * <p>OpenRouter exposes an OpenAI-compatible API, so this strategy reuses the
 * LangChain4J OpenAI model classes pointed at the OpenRouter base URL
 * ({@value #DEFAULT_BASE_URL}). The {@code endpoint} config field overrides the
 * base URL if set.
 *
 * <p>Model IDs use the OpenRouter namespaced form, e.g. {@code openai/gpt-4o},
 * {@code anthropic/claude-sonnet-4}, {@code deepseek/deepseek-r1}.
 *
 * <p>Supports chat (streaming and non-streaming) and embeddings. Image generation is
 * not supported: OpenRouter's {@code /api/v1/images} endpoint is not OpenAI-shaped.
 */
class OpenRouterModelProviderStrategy implements ModelProviderStrategy {

    static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    @Override
    public String providerName() {
        return "openrouter";
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return Set.of(Capability.CHAT, Capability.EMBEDDINGS);
    }

    @Override
    public List<ProviderField> configFields(final Capability capability) {
        return switch (capability) {
            case CHAT -> List.of(
                    ProviderField.required("apiKey", ProviderFieldType.SECRET),
                    ProviderField.required("model", ProviderFieldType.STRING, "Namespaced model ID, e.g. openai/gpt-4o"),
                    ProviderField.optional("endpoint", ProviderFieldType.STRING,
                            "Defaults to " + DEFAULT_BASE_URL),
                    ProviderField.optional("temperature", ProviderFieldType.NUMBER),
                    ProviderField.optional("maxTokens", ProviderFieldType.NUMBER),
                    ProviderField.optional("maxRetries", ProviderFieldType.NUMBER, "Not applied to streaming requests"),
                    ProviderField.optional("timeout", ProviderFieldType.NUMBER));
            case EMBEDDINGS -> List.of(
                    ProviderField.required("apiKey", ProviderFieldType.SECRET),
                    ProviderField.required("model", ProviderFieldType.STRING,
                            "Namespaced model ID, e.g. openai/text-embedding-3-small"),
                    ProviderField.optional("endpoint", ProviderFieldType.STRING,
                            "Defaults to " + DEFAULT_BASE_URL),
                    ProviderField.optional("dimensions", ProviderFieldType.NUMBER),
                    ProviderField.optional("maxRetries", ProviderFieldType.NUMBER),
                    ProviderField.optional("timeout", ProviderFieldType.NUMBER));
            case IMAGE -> throw new UnsupportedOperationException(
                    "OpenRouter image generation is not supported: the /api/v1/images endpoint "
                    + "is not OpenAI-shaped and cannot be driven by OpenAiImageModel.");
        };
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model())
                .baseUrl(baseUrl(config));
        if (config.temperature() != null) { builder.temperature(config.temperature()); }
        if (config.maxTokens() != null) { builder.maxTokens(config.maxTokens()); }
        if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model())
                .baseUrl(baseUrl(config));
        if (config.maxRetries() != null) {
            Logger.debug(OpenRouterModelProviderStrategy.class,
                    "maxRetries is not supported by the OpenRouter streaming chat model and will be ignored");
        }
        if (config.temperature() != null) { builder.temperature(config.temperature()); }
        if (config.maxTokens() != null) { builder.maxTokens(config.maxTokens()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model())
                .baseUrl(baseUrl(config));
        if (config.dimensions() != null) { builder.dimensions(config.dimensions()); }
        if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        return builder.build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "OpenRouter image generation is not supported: the /api/v1/images endpoint "
                + "is not OpenAI-shaped and cannot be driven by OpenAiImageModel.");
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.model(), "model", modelType);
        ModelProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
    }

    private static String baseUrl(final ProviderConfig config) {
        return config.endpoint() != null && !config.endpoint().isBlank()
                ? config.endpoint()
                : DEFAULT_BASE_URL;
    }

}
