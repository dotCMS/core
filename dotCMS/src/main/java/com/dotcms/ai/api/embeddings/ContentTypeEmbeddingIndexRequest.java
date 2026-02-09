package com.dotcms.ai.api.embeddings;

import com.dotcms.ai.config.AiModelConfig;

import java.util.Objects;
import java.util.Optional;

public class ContentTypeEmbeddingIndexRequest {

    private final String host;
    private final String contentType;
    private final Long languageId;
    private final int pageSize;
    private final int batchSize;
    private final String vendorModelPath;
    private final String indexName;
    private final AiModelConfig modelConfig;

    private ContentTypeEmbeddingIndexRequest(final Builder builder) {
        this.host = Objects.requireNonNull(builder.host, "host cannot be null");
        this.contentType = Objects.requireNonNull(builder.contentType, "contentType cannot be null");
        this.languageId = builder.languageId; // Optional field
        this.pageSize = builder.pageSize;
        this.batchSize = builder.batchSize;
        this.vendorModelPath = Objects.requireNonNull(builder.vendorModelPath, "vendorModelPath cannot be null");
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

    public String getVendorModelPath() {
        return vendorModelPath;
    }

    public String getIndexName() {
        return indexName;
    }

    public AiModelConfig getModelConfig() {
        return modelConfig;
    }

    // equals(), hashCode(), toString()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ContentTypeEmbeddingIndexRequest that = (ContentTypeEmbeddingIndexRequest) o;
        return pageSize == that.pageSize &&
                batchSize == that.batchSize &&
                Objects.equals(host, that.host) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(languageId, that.languageId) &&
                Objects.equals(vendorModelPath, that.vendorModelPath) &&
                Objects.equals(indexName, that.indexName) &&
                Objects.equals(modelConfig, that.modelConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, contentType, languageId, pageSize, batchSize, vendorModelPath, indexName, modelConfig);
    }

    @Override
    public String toString() {
        return "ContentTypeRagIndexRequest{" +
                "host='" + host + '\'' +
                ", contentType='" + contentType + '\'' +
                ", languageId=" + languageId +
                ", pageSize=" + pageSize +
                ", batchSize=" + batchSize +
                ", embeddingProviderKey='" + vendorModelPath + '\'' +
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
        private String vendorModelPath;
        private String indexName;
        private AiModelConfig modelConfig;

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

        public Builder withVendorModelPath(String vendorModelPath) {
            this.vendorModelPath = vendorModelPath;
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

        public ContentTypeEmbeddingIndexRequest build() {
            return new ContentTypeEmbeddingIndexRequest(this);
        }
    }
}
