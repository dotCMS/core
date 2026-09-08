package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for LangChain4J model construction.
 *
 * <p>Each AI provider implements this interface and is registered in
 * {@link LangChain4jModelFactory#STRATEGIES}. Adding a new provider requires only:
 * <ol>
 *   <li>Creating a new implementation of this interface, including
 *       {@link #supportedCapabilities()} and {@link #configFields(Capability)}</li>
 *   <li>Adding it to the {@code STRATEGIES} list in {@link LangChain4jModelFactory}</li>
 * </ol>
 * No other class needs to change — {@link LangChain4jModelFactory#listProviderMetadata()} picks up
 * the new provider's capabilities and fields automatically, so a REST client can render its
 * configuration form without any additional code.
 *
 * <p>The {@code modelType} parameter in each build method is the section name
 * ({@code "chat"}, {@code "embeddings"}, {@code "image"}) used solely for
 * error message context.
 */
interface ModelProviderStrategy {

    /**
     * Returns the provider identifier as it appears in {@code providerConfig} JSON,
     * e.g. {@code "openai"}, {@code "azure_openai"}. Case-insensitive matching is
     * performed by the factory.
     */
    String providerName();

    ChatModel buildChatModel(ProviderConfig config, String modelType);

    StreamingChatModel buildStreamingChatModel(ProviderConfig config, String modelType);

    EmbeddingModel buildEmbeddingModel(ProviderConfig config, String modelType);

    ImageModel buildImageModel(ProviderConfig config, String modelType);

    /**
     * Returns the capabilities this provider can serve. A {@code build*Model} method for a
     * capability not in this set throws {@link UnsupportedOperationException}.
     */
    Set<Capability> supportedCapabilities();

    /**
     * Returns the {@link ProviderConfig} fields this provider reads for the given capability,
     * with which are required. Only called for capabilities in {@link #supportedCapabilities()}.
     */
    List<ProviderField> configFields(Capability capability);

    /**
     * Shared validation helper available to all strategy implementations.
     */
    static void requireNonBlank(final String value, final String field, final String modelType) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "providerConfig." + modelType + "." + field + " is required but was not set");
        }
    }

}
