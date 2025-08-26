package com.dotcms.ai.v2.provider;

import com.dotcms.ai.v2.config.ModelConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class OpenAiModelProvider implements  ModelProvider {
    @Override
    public String name() {
        return Model.OPEN_AI_GPT_40.getProviderName();
    }

    @Override
    public ChatModel create(final ModelConfig config) {
        // this is a basic config, we have to work on the ChatMemory and so on.
        return OpenAiChatModel.builder()
                .apiKey(config.get(ModelConfig.API_KEY))
                .modelName(config.getOrDefault(ModelConfig.MODEL, Model.OPEN_AI_GPT_40.getModel()))
                .baseUrl(config.getOrDefault(ModelConfig.API_URL, Model.OPEN_AI_GPT_40.getApiUrl()))
                .build();
    }
}
