package com.dotcms.ai.api.embeddings;
import com.dotcms.ai.config.AiModelConfig;

import java.util.Objects;

public final class EmbeddingIndexRequest {

    private final String identifier;
    private final long languageId;
    private final String vendorModelPath;
    private final String indexName;
    private final AiModelConfig modelConfig;

    private EmbeddingIndexRequest(Builder builder) {
        this.identifier = Objects.requireNonNull(builder.identifier, "identifier cannot be null");
        this.languageId = builder.languageId;
        this.vendorModelPath = Objects.requireNonNull(builder.embeddingProviderKey, "vendorModelPath cannot be null");
        this.indexName = builder.indexName != null ? builder.indexName : "default";
        this.modelConfig = Objects.requireNonNull(builder.modelConfig, "modelConfig cannot be null");
    }

    // Getters
    public String getIdentifier() {
        return identifier;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getVendorModelPath() {
        return vendorModelPath;
    }

    public String getIndexName() {
        return indexName;
    }

    public AiModelConfig getModelConfig() {
        return modelConfig;
    }

    @Override
    public String toString() {
        return "EmbeddingIndexRequest{" +
                "identifier='" + identifier + '\'' +
                ", languageId=" + languageId +
                ", embeddingProviderKey='" + vendorModelPath + '\'' +
                ", indexName='" + indexName + '\'' +
                ", modelConfig=" + modelConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmbeddingIndexRequest that = (EmbeddingIndexRequest) o;
        return languageId == that.languageId &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(vendorModelPath, that.vendorModelPath) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(modelConfig, that.modelConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, languageId, vendorModelPath, indexName, modelConfig);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder of() {
        return new Builder()
                .withIdentifier(this.identifier)
                .withLanguageId(this.languageId)
                .withEmbeddingProviderKey(this.vendorModelPath)
                .withIndexName(this.indexName)
                .withModelConfig(this.modelConfig);
    }

    public static final class Builder {
        private String identifier;
        private long languageId;
        private String embeddingProviderKey;
        private String indexName;
        private AiModelConfig modelConfig;

        private Builder() {}

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withLanguageId(long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder withEmbeddingProviderKey(String embeddingProviderKey) {
            this.embeddingProviderKey = embeddingProviderKey;
            return this;
        }

        public Builder withIndexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder withModelConfig(AiModelConfig modelConfig) {
            this.modelConfig = modelConfig;
            return this;
        }

        public EmbeddingIndexRequest build() {
            return new EmbeddingIndexRequest(this);
        }
    }
}
