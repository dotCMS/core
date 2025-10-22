package com.dotcms.ai.api.provider;

import com.dotcms.ai.config.AiModelConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Provides a Chat Model based on a configuration
 * @author jsanca
 */
public interface ChatModelProvider {

    /**
     * Create a new ChatModel based on the config
     * @param config AiModelConfig
     * @return
     */
    ChatModel create(AiModelConfig config);

    /**
     * Create new Streaming ChatModel
     * @param config AiModelConfig
     * @return StreamingChatModel
     */
    StreamingChatModel createStreaming(AiModelConfig config);
}
