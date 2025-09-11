package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * A model provider creates a ChatModel based on a ModelConfig
 * @author jsanca
 */
public interface ModelProvider {

    String name(); // e.g., "openai", "azure", "ollama"
    ChatModel create(ModelConfig config);
    StreamingChatModel createStreaming(ModelConfig config);
    EmbeddingModel createEmbedding(ModelConfig config);
}
