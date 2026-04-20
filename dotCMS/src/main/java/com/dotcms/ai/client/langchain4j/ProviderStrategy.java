package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

/**
 * Strategy interface for building LangChain4J model instances for a specific AI provider.
 *
 * <p>Each implementation encapsulates all provider-specific builder logic.
 * To add a new provider, implement this interface and register it in
 * {@link LangChain4jModelFactory#STRATEGIES}.
 */
public interface ProviderStrategy {

    /**
     * Returns the lowercase identifier used in {@code providerConfig.provider}
     * (e.g. {@code "openai"}, {@code "bedrock"}).
     */
    String providerName();

    /**
     * Validates that all required fields for the given model type are present in the config.
     *
     * @throws IllegalArgumentException if a required field is missing
     */
    void validate(ProviderConfig config, String modelType);

    /**
     * Builds a synchronous chat model.
     */
    ChatModel buildChatModel(ProviderConfig config);

    /**
     * Builds a streaming chat model.
     */
    StreamingChatModel buildStreamingChatModel(ProviderConfig config);

    /**
     * Builds an embedding model.
     *
     * @throws UnsupportedOperationException if the provider does not support embeddings
     */
    EmbeddingModel buildEmbeddingModel(ProviderConfig config);

    /**
     * Builds an image generation model.
     *
     * @throws UnsupportedOperationException if the provider does not support image generation
     */
    ImageModel buildImageModel(ProviderConfig config);

    /**
     * Shared utility to validate that a required config field is non-blank.
     */
    static void requireNonBlank(final String value, final String field, final String modelType) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "providerConfig." + modelType + "." + field + " is required but was not set");
        }
    }

}
