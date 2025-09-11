package com.dotcms.ai.v2.api.embeddings;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;

import java.util.Objects;

public final class SingleRagIndexRequest {

    private final String identifier;
    private final long languageId;
    private final String embeddingProviderKey;
    private final String indexName;
    private final ModelConfig modelConfig;

    private SingleRagIndexRequest(Builder builder) {
        this.identifier = Objects.requireNonNull(builder.identifier, "identifier cannot be null");
        this.languageId = builder.languageId;
        this.embeddingProviderKey = Objects.requireNonNull(builder.embeddingProviderKey, "embeddingProviderKey cannot be null");
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

    public String getEmbeddingProviderKey() {
        return embeddingProviderKey;
    }

    public String getIndexName() {
        return indexName;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    @Override
    public String toString() {
        return "RagIndexRequest{" +
                "identifier='" + identifier + '\'' +
                ", languageId=" + languageId +
                ", embeddingProviderKey='" + embeddingProviderKey + '\'' +
                ", indexName='" + indexName + '\'' +
                ", modelConfig=" + modelConfig +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SingleRagIndexRequest that = (SingleRagIndexRequest) o;
        return languageId == that.languageId &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(embeddingProviderKey, that.embeddingProviderKey) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(modelConfig, that.modelConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, languageId, embeddingProviderKey, indexName, modelConfig);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder of() {
        return new Builder()
                .withIdentifier(this.identifier)
                .withLanguageId(this.languageId)
                .withEmbeddingProviderKey(this.embeddingProviderKey)
                .withIndexName(this.indexName)
                .withModelConfig(this.modelConfig);
    }

    public static final class Builder {
        private String identifier;
        private long languageId;
        private String embeddingProviderKey;
        private String indexName;
        private ModelConfig modelConfig;

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

        public Builder withModelConfig(ModelConfig modelConfig) {
            this.modelConfig = modelConfig;
            return this;
        }

        public SingleRagIndexRequest build() {
            return new SingleRagIndexRequest(this);
        }
    }
}
