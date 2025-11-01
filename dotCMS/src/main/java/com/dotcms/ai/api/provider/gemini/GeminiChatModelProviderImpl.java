package com.dotcms.ai.api.provider.gemini;

import com.dotcms.ai.api.provider.ChatModelProvider;
import com.dotcms.ai.config.AiModel;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.time.Duration;

/**
 * Chat Model provider for Google Gemini.
 * Supports both standard and streaming chat models.
 *
 * @author jsanca
 */
public class GeminiChatModelProviderImpl implements ChatModelProvider {

    @Override
    public ChatModel create(final AiModelConfig config) {

        final double temperature = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"), 0.3);
        final long timeoutMs = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"), 30000L);
        final int maxTokens = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, "2048"), 2048);

        return GoogleAiGeminiChatModel.builder()
                .apiKey(config.get(AiModelConfig.API_KEY))
                .modelName(config.getOrDefault(AiModelConfig.MODEL, AiModel.GEMINI_1_5_FLASH.getModel()))
                .baseUrl(config.getOrDefault(AiModelConfig.API_URL, AiModel.GEMINI_1_5_FLASH.getApiUrl()))
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {

        final double temperature = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"), 0.3);
        final long timeoutMs = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"), 30000L);
        final int maxTokens = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, "2048"), 2048);

        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(config.get(AiModelConfig.API_KEY))
                .modelName(config.getOrDefault(AiModelConfig.MODEL, AiModel.GEMINI_1_5_FLASH.getModel()))
                .baseUrl(config.getOrDefault(AiModelConfig.API_URL, AiModel.GEMINI_1_5_FLASH.getApiUrl()))
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
