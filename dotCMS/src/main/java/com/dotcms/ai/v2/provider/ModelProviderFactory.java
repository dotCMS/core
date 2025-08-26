package com.dotcms.ai.v2.provider;

import com.dotcms.ai.v2.config.ModelConfig;
import dev.langchain4j.model.chat.ChatModel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
        this(List.of(new OpenAiModelProvider()));
    }

    public ModelProviderFactory(final List<ModelProvider> providerList) {
        for (final ModelProvider provider : providerList) {
            providers.put(provider.name(), provider);
        }
    }

    // todo: add osgi support


    public ChatModel get(final String providerName, final ModelConfig config) {

        final ModelProvider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown model provider: " + providerName);
        }
        return provider.create(config);
    }
}
