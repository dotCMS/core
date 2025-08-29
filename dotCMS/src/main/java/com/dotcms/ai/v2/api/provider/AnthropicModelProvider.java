package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;

public class AnthropicModelProvider implements ModelProvider {

    @Override
    public String name() {
        return Model.ANTHROPIC_CLAUDE_3_7.getProviderName();
    }

    @Override
    public ChatModel create(final ModelConfig config) {
        // this is a basic config, we have to work on the ChatMemory and so on.
        return AnthropicChatModel.builder()
                .apiKey(config.get(ModelConfig.API_KEY))
                .beta("prompt-caching-2024-07-31") // ????
                .modelName(config.get(ModelConfig.MODEL))
                .cacheSystemMessages(true)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
