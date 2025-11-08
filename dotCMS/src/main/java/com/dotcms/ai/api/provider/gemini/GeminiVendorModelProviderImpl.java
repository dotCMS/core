package com.dotcms.ai.api.provider.gemini;

import com.dotcms.ai.api.provider.EmbeddingModelProvider;
import com.dotcms.ai.api.provider.VendorModelProvider;
import com.dotcms.ai.api.provider.vertex.VertexChatModelProviderImpl;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiVendor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class GeminiVendorModelProviderImpl implements VendorModelProvider {

    private final GeminiChatModelProviderImpl chatModelProvider =  new GeminiChatModelProviderImpl();
    private final EmbeddingModelProvider embeddingModelProvider =  new GeminiEmbeddingModelProviderImpl();

    @Override
    public String getVendorName() {
        return AiVendor.GEMINI.getVendorName();
    }

    @Override
    public ChatModel create(final AiModelConfig config) {
        return chatModelProvider.create(config);
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {
        return chatModelProvider.createStreaming(config);
    }

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {
        return embeddingModelProvider.createEmbedding(config);
    }
}
