package com.dotcms.ai.v2.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@JsonDeserialize(builder = RagSearchRequest.Builder.class)
public final class RagSearchRequest {

    private final String query;
    private final String site;
    private final List<String> contentTypes;
    private final String languageId;
    private final String identifier;
    private final Integer limit;
    private final Integer offset;
    private final Double threshold;
    private final Map<String, Object> options;

    private RagSearchRequest(Builder builder) {
        this.query = Objects.requireNonNull(builder.query, "Query cannot be null");
        this.site = builder.site;
        this.contentTypes = builder.contentTypes != null ? List.copyOf(builder.contentTypes) : Collections.emptyList();
        this.languageId = builder.languageId;
        this.identifier = builder.identifier;
        this.limit = builder.limit != null ? builder.limit : 8;
        this.offset = builder.offset != null ? builder.offset : 0;
        this.threshold = builder.threshold != null ? builder.threshold : 0.75;
        this.options = builder.options != null ? Map.copyOf(builder.options) : Collections.emptyMap();
    }

    public String getQuery() {
        return query;
    }

    public Optional<String> getSite() {
        return Optional.ofNullable(site);
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public Optional<String> getLanguageId() {
        return Optional.ofNullable(languageId);
    }

    public Optional<String> getIdentifier() {
        return Optional.ofNullable(identifier);
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public Double getThreshold() {
        return threshold;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RagSearchRequest that = (RagSearchRequest) o;
        return Objects.equals(query, that.query) &&
                Objects.equals(site, that.site) &&
                Objects.equals(contentTypes, that.contentTypes) &&
                Objects.equals(languageId, that.languageId) &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(limit, that.limit) &&
                Objects.equals(offset, that.offset) &&
                Objects.equals(threshold, that.threshold) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, site, contentTypes, languageId, identifier, limit, offset, threshold, options);
    }

    @Override
    public String toString() {
        return "RagSearchRequest{" +
                "query='" + query + '\'' +
                ", site='" + site + '\'' +
                ", contentTypes=" + contentTypes +
                ", languageId='" + languageId + '\'' +
                ", identifier='" + identifier + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", threshold=" + threshold +
                ", options=" + options +
                '}';
    }

    public static final class Builder {
        @JsonProperty(required = true)
        private String query;
        @JsonProperty()
        private String site;
        @JsonProperty()
        private List<String> contentTypes;
        @JsonProperty()
        private String languageId;
        @JsonProperty()
        private String identifier;
        @JsonProperty()
        private Integer limit;
        @JsonProperty()
        private Integer offset;
        @JsonProperty()
        private Double threshold;
        @JsonProperty()
        private Map<String, Object> options;

        private Builder() {}

        public Builder withQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder withSite(String site) {
            this.site = site;
            return this;
        }

        public Builder withContentTypes(List<String> contentTypes) {
            this.contentTypes = contentTypes;
            return this;
        }

        public Builder withLanguageId(String languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withLimit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder withOffset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder withThreshold(Double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder withOptions(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public RagSearchRequest build() {
            return new RagSearchRequest(this);
        }
    }
}
