package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.List;

/**
 * Factory for creating LangChain4J model instances from a {@link ProviderConfig}.
 *
 * <p>Provider-specific logic lives in {@link ModelProviderStrategy} implementations.
 * To add a new provider, create a class that implements {@link ModelProviderStrategy}
 * and add an instance to {@link #STRATEGIES}. No other class needs to change.
 *
 * <p>Supported providers: {@code openai}, {@code azure_openai}, {@code vertex_ai}
 * <p>Note: {@code vertex_ai} supports chat only; embeddings and image are not available via LangChain4J.
 */
public class LangChain4jModelFactory {

    static final List<ModelProviderStrategy> STRATEGIES = List.of(
            new OpenAiModelProviderStrategy(),
            new AzureOpenAiModelProviderStrategy(),
            new VertexAiModelProviderStrategy()
    );

    private LangChain4jModelFactory() {}

    /**
     * Builds a {@link ChatModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the chat section
     * @return a configured {@link ChatModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static ChatModel buildChatModel(final ProviderConfig config) {
        return resolve(config, "chat").buildChatModel(config, "chat");
    }

    /**
     * Builds a {@link StreamingChatModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the chat section
     * @return a configured {@link StreamingChatModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        return resolve(config, "chat").buildStreamingChatModel(config, "chat");
    }

    /**
     * Builds an {@link EmbeddingModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the embeddings section
     * @return a configured {@link EmbeddingModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        return resolve(config, "embeddings").buildEmbeddingModel(config, "embeddings");
    }

    /**
     * Builds an {@link ImageModel} for the given provider configuration.
     *
     * @param config provider-specific configuration for the image section
     * @return a configured {@link ImageModel}
     * @throws IllegalArgumentException if config or provider is null, or the provider is unsupported
     */
    public static ImageModel buildImageModel(final ProviderConfig config) {
        return resolve(config, "image").buildImageModel(config, "image");
    }

    private static ModelProviderStrategy resolve(final ProviderConfig config, final String modelType) {
        if (config == null || config.provider() == null) {
            throw new IllegalArgumentException(
                    "ProviderConfig or provider name is null for model type: " + modelType);
        }
        final String provider = config.provider().toLowerCase();
        return STRATEGIES.stream()
                .filter(s -> s.providerName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported " + modelType + " provider: " + config.provider()
                        + ". Supported: " + supportedProviders()));
    }

    private static String supportedProviders() {
        final StringBuilder sb = new StringBuilder();
        for (final ModelProviderStrategy s : STRATEGIES) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.providerName());
        }
        return sb.toString();
    }

}
