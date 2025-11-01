package com.dotcms.ai.api.provider.azure;

import com.dotcms.ai.api.provider.EmbeddingModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.time.Duration;

/**
 * Azure OpenAI Embedding Model Provider Implementation
 * Supports embedding models on Azure OpenAI Service.
 */
public class AzureEmbeddingModelProviderImpl implements EmbeddingModelProvider {

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {

        final String apiKey     = config.get(AiModelConfig.API_KEY);
        final String endpoint   = config.getOrDefault(AiModelConfig.API_URL, "");
        final String deployment = config.getOrDefault("deploymentName", "");
        final long timeoutMs    = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"),
                20000L
        );

        return AzureOpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .deploymentName(deployment)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
