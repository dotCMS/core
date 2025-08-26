package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import dev.langchain4j.model.chat.ChatModel;

/**
 * A model provider creates a ChatModel based on a ModelConfig
 * @author jsanca
 */
public interface ModelProvider {

    String name(); // e.g., "openai", "azure", "ollama"
    ChatModel create(ModelConfig config);
}
