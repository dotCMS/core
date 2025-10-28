package com.dotcms.ai.api.provider.azure;

import com.dotcms.ai.api.provider.VendorModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiVendor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class AzureVendorModelProviderImpl implements VendorModelProvider {

    private final AzureChatModelProviderImpl azureChatModelProvider =  new AzureChatModelProviderImpl();
    private final AzureEmbeddingModelProviderImpl embeddingProvider = new AzureEmbeddingModelProviderImpl();

    @Override
    public String getVendorName() {
        return AiVendor.AZURE_OPEN_AI.getVendorName();
    }

    @Override
    public ChatModel create(final AiModelConfig config) {
        return azureChatModelProvider.create(config);
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {
        return azureChatModelProvider.createStreaming(config);
    }

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {
        return embeddingProvider.createEmbedding(config);
    }
}
