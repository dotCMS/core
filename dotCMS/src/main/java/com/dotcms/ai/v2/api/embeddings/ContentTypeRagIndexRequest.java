package com.dotcms.ai.v2.api.embeddings;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;

import java.util.Objects;
import java.util.Optional;

public final class ContentTypeRagIndexRequest {

    private final String host;
    private final String contentType;
    private final Long languageId;
    private final int pageSize;
    private final int batchSize;
    private final String embeddingProviderKey;
    private final String indexName;
    private final ModelConfig modelConfig;

    private ContentTypeRagIndexRequest(Builder builder) {
        this.host = Objects.requireNonNull(builder.host, "host cannot be null");
        this.contentType = Objects.requireNonNull(builder.contentType, "contentType cannot be null");
        this.languageId = builder.languageId; // Optional field
        this.pageSize = builder.pageSize;
        this.batchSize = builder.batchSize;
        this.embeddingProviderKey = Objects.requireNonNull(builder.embeddingProviderKey, "embeddingProviderKey cannot be null");
        this.indexName = builder.indexName != null ? builder.indexName : "default";
        this.modelConfig = Objects.requireNonNull(builder.modelConfig, "modelConfig cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getHost() {
        return host;
    }

    public String getContentType() {
        return contentType;
    }

    public Optional<Long> getLanguageId() {
        return Optional.ofNullable(languageId);
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getBatchSize() {
        return batchSize;
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

    // equals(), hashCode(), toString()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentTypeRagIndexRequest that = (ContentTypeRagIndexRequest) o;
        return pageSize == that.pageSize &&
                batchSize == that.batchSize &&
                Objects.equals(host, that.host) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(languageId, that.languageId) &&
                Objects.equals(embeddingProviderKey, that.embeddingProviderKey) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(modelConfig, that.modelConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, contentType, languageId, pageSize, batchSize, embeddingProviderKey, indexName, modelConfig);
    }

    @Override
    public String toString() {
        return "ContentTypeRagIndexRequest{" +
                "host='" + host + '\'' +
                ", contentType='" + contentType + '\'' +
                ", languageId=" + languageId +
                ", pageSize=" + pageSize +
                ", batchSize=" + batchSize +
                ", embeddingProviderKey='" + embeddingProviderKey + '\'' +
                ", indexName='" + indexName + '\'' +
                ", modelConfig=" + modelConfig +
                '}';
    }

    // Builder Class
    public static final class Builder {
        private String host;
        private String contentType;
        private Long languageId;
        private int pageSize;
        private int batchSize;
        private String embeddingProviderKey;
        private String indexName;
        private ModelConfig modelConfig;

        private Builder() {}

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withLanguageId(Long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder withPageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
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

        public ContentTypeRagIndexRequest build() {
            return new ContentTypeRagIndexRequest(this);
        }
    }
}
