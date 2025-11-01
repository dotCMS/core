package com.dotcms.ai.api.provider.anthropic;

import com.dotcms.ai.api.provider.VendorModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.ai.config.AiVendor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class AnthropicVendorModelProviderImpl implements VendorModelProvider {

    private final AnthropicChatModelProviderImpl anthropicChatModelProvider =  new AnthropicChatModelProviderImpl();

    @Override
    public String getVendorName() {
        return AiVendor.ANTHROPIC.getVendorName();
    }

    @Override
    public ChatModel create(final AiModelConfig config) {
        return anthropicChatModelProvider.create(config);
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {
        return anthropicChatModelProvider.createStreaming(config);
    }

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {
        throw new UnsupportedOperationException("Anthropic vendor model provider does not support embedding");
    }
}
