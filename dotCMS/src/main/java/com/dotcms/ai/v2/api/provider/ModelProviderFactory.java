package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory to create the AI Model Providers
 * @author jsanca
 */
@ApplicationScoped
public class ModelProviderFactory {

    private final Map<String, ModelProvider> providers = new ConcurrentHashMap<>();

    public ModelProviderFactory() {
        this(List.of(new OpenAiModelProvider(), // Open AI LLM and Streaming
                new OpenAiModelProvider(Model.OPEN_AI_GPT_40.getProviderName()), // Open AI Embbeding model
                new AnthropicModelProvider()));
    }

    public ModelProviderFactory(final List<ModelProvider> providerList) {
        for (final ModelProvider provider : providerList) {
            providers.put(provider.name(), provider);
        }
    }

    // todo: add osgi support probably will need a proxy that gets injected this class and exposes statically to the activator
    public void addProvider(final ModelProvider provider) {
        providers.put(provider.name(), provider);
    }

    public ChatModel get(final String providerName, final ModelConfig config) {

        final ModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // todo: if eventually have a default one, by config on, use instead
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.create(config);
    }

    public StreamingChatModel getStreaming(final String providerName, final ModelConfig config) {

        final ModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // todo: if eventually have a default one, by config on, use instead
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.createStreaming(config);
    }

    public EmbeddingModel getEmbedding(final String providerName, final ModelConfig config) {

        final ModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // todo: if eventually have a default one, by config on, use instead
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.createEmbedding(config);
    }
}
