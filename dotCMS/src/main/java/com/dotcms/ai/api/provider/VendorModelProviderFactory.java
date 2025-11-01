package com.dotcms.ai.api.provider;

import com.dotcms.ai.api.provider.anthropic.AnthropicVendorModelProviderImpl;
import com.dotcms.ai.api.provider.azure.AzureVendorModelProviderImpl;
import com.dotcms.ai.api.provider.bedrock.BedRockVendorModelProviderImpl;
import com.dotcms.ai.api.provider.openai.OpenAiVendorModelProviderImpl;
import com.dotcms.ai.config.AiModelConfig;
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
public class VendorModelProviderFactory {

    private final Map<String, VendorModelProvider> providers = new ConcurrentHashMap<>();

    public VendorModelProviderFactory() {
        this(List.of(new OpenAiVendorModelProviderImpl(), new AnthropicVendorModelProviderImpl(),
                new AzureVendorModelProviderImpl(), new BedRockVendorModelProviderImpl()));
    }

    public VendorModelProviderFactory(final List<VendorModelProvider> providerList) {
        for (final VendorModelProvider provider : providerList) {
            addProvider(provider);
        }
    }

    // todo: add osgi support probably will need a proxy that gets injected this class and exposes statically to the activator
    public void addProvider(final VendorModelProvider provider) {
        providers.put(provider.getVendorName().toLowerCase(), provider);
    }

    public ChatModel get(final String providerName, final AiModelConfig config) {

        final VendorModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // todo: if eventually have a default one, by config on, use instead
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.create(config);
    }

    public StreamingChatModel getStreaming(final String providerName, final AiModelConfig config) {

        final VendorModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // todo: if eventually have a default one, by config on, use instead
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.createStreaming(config);
    }

    public EmbeddingModel getEmbedding(final String providerName, final AiModelConfig config) {

        final VendorModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            // todo: if eventually have a default one, by config on, use instead
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.createEmbedding(config);
    }
}
