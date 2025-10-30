package com.dotcms.ai.api.provider.gemini;

import com.dotcms.ai.api.provider.EmbeddingModelProvider;
import com.dotcms.ai.config.AiModel;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;

import java.time.Duration;

/**
 * Embedding model provider for Google Gemini.
 * Supports text and multimodal embeddings where available.
 *
 * @author jsanca
 */
public class GeminiEmbeddingModelProviderImpl implements EmbeddingModelProvider {

    private static final int DEFAULT_DIMENSION = 768;
    private static final String DEFAULT_DIMENSION_AS_STRING = String.valueOf(DEFAULT_DIMENSION);

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {

        final int dimensions = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, DEFAULT_DIMENSION_AS_STRING),
                DEFAULT_DIMENSION);
        final long timeoutMs = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "20000"), 20000L);

        return GoogleAiEmbeddingModel.builder()
                .apiKey(config.get(AiModelConfig.API_KEY))
                .modelName(config.getOrDefault(AiModelConfig.MODEL, AiModel.GEMINI_1_5_FLASH.getModel()))
                .baseUrl(config.getOrDefault(AiModelConfig.API_URL, AiModel.GEMINI_1_5_FLASH.getApiUrl()))
                .outputDimensionality(dimensions)
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
