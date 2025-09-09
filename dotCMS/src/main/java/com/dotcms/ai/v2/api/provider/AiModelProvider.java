package com.dotcms.ai.v2.api.provider;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * Ai Model Provider for Quarkus
 * @author jsanca
 */
@ApplicationScoped
public class AiModelProvider {

    /**
     * Returns the default embedding model onnx
     * @return EmbeddingModel
     */
    @Produces
    @ApplicationScoped
    @Named("onnx")
    public EmbeddingModel createDefaultEmbeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
