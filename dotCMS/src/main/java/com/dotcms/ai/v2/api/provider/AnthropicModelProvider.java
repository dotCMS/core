package com.dotcms.ai.v2.api.provider;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

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

    @Override
    public StreamingChatModel createStreaming(final ModelConfig config) {

        return AnthropicStreamingChatModel.builder()
                .apiKey(System.getenv(config.get(ModelConfig.API_KEY)))
                .modelName(config.get(ModelConfig.MODEL)) // "claude-3-haiku-20240307"
                .logRequests(true)
                // Other parameters can be set as well
                .build();
    }

    @Override
    public EmbeddingModel createEmbedding(final ModelConfig config) {


        return new AllMiniLmL6V2EmbeddingModel(); // diay si
    }
}
