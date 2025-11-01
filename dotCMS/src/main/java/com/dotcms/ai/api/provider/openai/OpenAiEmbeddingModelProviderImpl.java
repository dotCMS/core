package com.dotcms.ai.api.provider.openai;

import com.dotcms.ai.api.provider.EmbeddingModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.time.Duration;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
/**
 * Embedding model provider for open ai
 * @author jsanca
 */
public class OpenAiEmbeddingModelProviderImpl implements EmbeddingModelProvider {

    private static final int DEFAULT_DIMENSION = TEXT_EMBEDDING_3_SMALL.dimension();
    private static final String DEFAULT_DIMENSION_AS_STRING = String.valueOf(DEFAULT_DIMENSION);

    @Override
    public EmbeddingModel createEmbedding(final AiModelConfig config) {

        final int dimensions = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.DIMENSIONS, DEFAULT_DIMENSION_AS_STRING), DEFAULT_DIMENSION);

        return OpenAiEmbeddingModel.builder()
                .apiKey(config.get(AiModelConfig.API_KEY))
                .modelName(config.getOrDefault(AiModelConfig.MODEL, TEXT_EMBEDDING_3_SMALL.name()))
                .dimensions(dimensions)
                .timeout(Duration.ofMillis(ConversionUtils.toLong(
                        config.getOrDefault(AiModelConfig.TIMEOUT_MS, "20000"), 20000l)))
                .build();
    }
}
