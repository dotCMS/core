package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;
import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;

public class OpenAiModelProvider implements  ModelProvider {

    private final String name;

    public OpenAiModelProvider() {
        this(Model.OPEN_AI_GPT_40.getProviderName());
    }
    public OpenAiModelProvider(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
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

    @Override
    public StreamingChatModel createStreaming(final ModelConfig config) {

        return OpenAiStreamingChatModel.builder()
                .apiKey(config.get(ModelConfig.API_KEY))
                .modelName(config.getOrDefault(ModelConfig.MODEL, Model.OPEN_AI_GPT_40.getModel()))
                .baseUrl(config.getOrDefault(ModelConfig.API_URL, Model.OPEN_AI_GPT_40.getApiUrl()))
                .build();
    }

    @Override
    public EmbeddingModel createEmbedding(final ModelConfig config) {

        return OpenAiEmbeddingModel.builder()
                .apiKey(config.get(ModelConfig.API_KEY))
                .modelName(config.getOrDefault(ModelConfig.MODEL, TEXT_EMBEDDING_3_SMALL.name()))
                .build();
    }
}
