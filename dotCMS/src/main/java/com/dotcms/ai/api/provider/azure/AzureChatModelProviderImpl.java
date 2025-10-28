package com.dotcms.ai.api.provider.azure;

import com.dotcms.ai.api.provider.ChatModelProvider;
import com.dotcms.ai.config.AiModel;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.time.Duration;

/**
 * Azure OpenAI Chat Model Provider Implementation
 * Supports chat models on Azure OpenAI Service.
 */
public class AzureChatModelProviderImpl implements ChatModelProvider {

    @Override
    public ChatModel create(final AiModelConfig config) {

        final String apiKey        = config.get(AiModelConfig.API_KEY);
        final String endpoint      = config.getOrDefault(AiModelConfig.API_URL, "");
        final String deployment    = config.getOrDefault("deploymentName", "");
        final double temperature   = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"),
                0.3
        );
        final long timeoutMs       = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"),
                30000L
        );

        return AzureOpenAiChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deployment)
                .temperature(temperature)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {

        final String apiKey        = config.get(AiModelConfig.API_KEY);
        final String endpoint      = config.getOrDefault(AiModelConfig.API_URL, "");
        final String deployment    = config.getOrDefault("deploymentName", "");
        final double temperature   = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"),
                0.3
        );
        final long timeoutMs       = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"),
                30000L
        );

        return AzureOpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deployment)
                .temperature(temperature)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
