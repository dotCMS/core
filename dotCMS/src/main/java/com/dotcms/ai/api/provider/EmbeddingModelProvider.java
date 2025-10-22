package com.dotcms.ai.api.provider;

import com.dotcms.ai.config.AiModelConfig;
import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * Provides an Embedding Model based on a configuration
 * @author jsanca
 */
public interface EmbeddingModelProvider {

    /**
     * Creates embedding based on a configuration
     * @param config
     * @return EmbeddingModel
     */
    EmbeddingModel createEmbedding(AiModelConfig config);
}
