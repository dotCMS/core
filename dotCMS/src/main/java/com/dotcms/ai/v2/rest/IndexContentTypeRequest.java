package com.dotcms.ai.v2.rest;

import java.util.Objects;
import java.util.Optional;

public final class IndexContentTypeRequest {

    private final String host;
    private final String contentType;
    private final Long languageId;
    private final Integer pageSize;
    private final Integer batchSize;

    private IndexContentTypeRequest(Builder builder) {
        this.host = builder.host;
        this.contentType = Objects.requireNonNull(builder.contentType, "contentType cannot be null");
        this.languageId = builder.languageId;
        this.pageSize = builder.pageSize;
        this.batchSize = builder.batchSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public String getContentType() {
        return contentType;
    }

    public Optional<Long> getLanguageId() {
        return Optional.ofNullable(languageId);
    }

    public Optional<Integer> getPageSize() {
        return Optional.ofNullable(pageSize);
    }

    public Optional<Integer> getBatchSize() {
        return Optional.ofNullable(batchSize);
    }

    // equals(), hashCode(), toString()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexContentTypeRequest that = (IndexContentTypeRequest) o;
        return Objects.equals(host, that.host) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(languageId, that.languageId) &&
                Objects.equals(pageSize, that.pageSize) &&
                Objects.equals(batchSize, that.batchSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, contentType, languageId, pageSize, batchSize);
    }

    @Override
    public String toString() {
        return "IndexContentTypeRequest{" +
                "host='" + host + '\'' +
                ", contentType='" + contentType + '\'' +
                ", languageId=" + languageId +
                ", pageSize=" + pageSize +
                ", batchSize=" + batchSize +
                '}';
    }

    // Builder Class
    public static final class Builder {
        private String host;
        private String contentType;
        private Long languageId;
        private Integer pageSize;
        private Integer batchSize;

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

        public Builder withPageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder withBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public IndexContentTypeRequest build() {
            return new IndexContentTypeRequest(this);
        }
    }
}
