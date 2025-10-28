package com.dotcms.ai.api.provider.bedrock;

import com.dotcms.ai.api.provider.VendorModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiVendor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class BedRockVendorModelProviderImpl implements VendorModelProvider {

    private final BedrockChatModelProviderImpl bedrockChatModelProvider =  new BedrockChatModelProviderImpl();
    private final BedrockTitanEmbeddingModelProviderImpl bedrockTitanEmbeddingModelProvider = new BedrockTitanEmbeddingModelProviderImpl();

    @Override
    public String getVendorName() {
        return AiVendor.OPEN_AI.getVendorName();
    }

    @Override
    public ChatModel create(final AiModelConfig config) {
        return bedrockChatModelProvider.create(config);
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {
        return bedrockChatModelProvider.createStreaming(config);
    }

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {
        return bedrockTitanEmbeddingModelProvider.createEmbedding(config);
    }
}
