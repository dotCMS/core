package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.Map;

/**
 * Factory for creating LangChain4J model instances from a {@link ProviderConfig}.
 *
 * <p>Delegates to the appropriate {@link ProviderStrategy} based on {@code config.provider()}.
 * To add support for a new provider, implement {@link ProviderStrategy} and add an entry
 * to {@link #STRATEGIES} — no other class needs to change.
 *
 * <p>Supported providers: {@code openai}, {@code azure_openai}, {@code bedrock}, {@code vertex_ai}
 */
public class LangChain4jModelFactory {

    static final Map<String, ProviderStrategy> STRATEGIES = Map.of(
            "openai",       new OpenAiProviderStrategy(),
            "azure_openai", new AzureOpenAiProviderStrategy(),
            "bedrock",      new BedrockProviderStrategy(),
            "vertex_ai",    new VertexAiProviderStrategy()
    );

    private LangChain4jModelFactory() {}

    public static ChatModel buildChatModel(final ProviderConfig config) {
        return resolve(config, "chat").buildChatModel(config);
    }

    public static StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        return resolve(config, "chat").buildStreamingChatModel(config);
    }

    public static EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        return resolve(config, "embeddings").buildEmbeddingModel(config);
    }

    public static ImageModel buildImageModel(final ProviderConfig config) {
        return resolve(config, "image").buildImageModel(config);
    }

    private static ProviderStrategy resolve(final ProviderConfig config, final String modelType) {
        if (config == null || config.provider() == null) {
            throw new IllegalArgumentException(
                    "ProviderConfig or provider name is null for model type: " + modelType);
        }
        ProviderStrategy.requireNonBlank(config.model(), "model", modelType);
        final ProviderStrategy strategy = STRATEGIES.get(config.provider().toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported " + modelType + " provider: "
                    + config.provider() + ". Supported: " + String.join(", ", STRATEGIES.keySet()));
        }
        strategy.validate(config, modelType);
        return strategy;
    }

}
