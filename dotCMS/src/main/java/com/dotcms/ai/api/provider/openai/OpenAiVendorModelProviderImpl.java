package com.dotcms.ai.api.provider.openai;

import com.dotcms.ai.api.provider.VendorModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiVendor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class OpenAiVendorModelProviderImpl implements VendorModelProvider {

    private final OpenAiChatModelProviderImpl openAiChatModelProvider =  new OpenAiChatModelProviderImpl();
    private final OpenAiEmbeddingModelProviderImpl openAiEmbeddingModelProvider = new OpenAiEmbeddingModelProviderImpl();

    @Override
    public String getVendorName() {
        return AiVendor.OPEN_AI.getVendorName();
    }

    @Override
    public ChatModel create(final AiModelConfig config) {
        return openAiChatModelProvider.create(config);
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {
        return openAiChatModelProvider.createStreaming(config);
    }

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {
        return openAiEmbeddingModelProvider.createEmbedding(config);
    }
}
