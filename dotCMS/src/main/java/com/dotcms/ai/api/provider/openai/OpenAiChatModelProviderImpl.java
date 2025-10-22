package com.dotcms.ai.api.provider.openai;

import com.dotcms.ai.api.provider.ChatModelProvider;
import com.dotcms.ai.config.AiModel;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.time.Duration;

/**
 * Open Ai Implementation
 * @author jsanc
 */
public class OpenAiChatModelProviderImpl implements ChatModelProvider {

    @Override
    public ChatModel create(final AiModelConfig config) {
        // this is a basic config, we have to work on the ChatMemory and so on.
        return OpenAiChatModel.builder()
                .apiKey(config.get(AiModelConfig.API_KEY))
                .modelName(config.getOrDefault(AiModelConfig.MODEL, AiModel.OPEN_AI_GPT_4O_MINI.getModel()))
                .baseUrl(config.getOrDefault(AiModelConfig.API_URL, AiModel.OPEN_AI_GPT_4O_MINI.getApiUrl()))
                .temperature(ConversionUtils.toDouble(
                        config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"), 0.3))
                .timeout(Duration.ofMillis(ConversionUtils.toLong(
                        config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"), 30000l)))
                .maxTokens(ConversionUtils.toInt(
                        config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, "2048"), 2048))

                // todo: on next steps we will add more logic such as fallbacks + proxies
                .build();
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {

        return OpenAiStreamingChatModel.builder()
                .apiKey(config.get(AiModelConfig.API_KEY))
                .modelName(config.getOrDefault(AiModelConfig.MODEL, AiModel.OPEN_AI_GPT_4O_MINI.getModel()))
                .baseUrl(config.getOrDefault(AiModelConfig.API_URL, AiModel.OPEN_AI_GPT_4O_MINI.getApiUrl()))
                .temperature(ConversionUtils.toDouble(
                        config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"), 0.3))
                .timeout(Duration.ofMillis(ConversionUtils.toLong(
                        config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"), 30000l)))
                .maxTokens(ConversionUtils.toInt(
                        config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, "2048"), 2048))

                .build();
    }
}
