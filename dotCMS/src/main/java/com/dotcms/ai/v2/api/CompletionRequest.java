package com.dotcms.ai.v2.api;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.Map;
import java.util.List;
import java.util.Objects;

/**
 * One-shot completion request. It can be pure generation (prompt-only) or
 * Retrieval-Augmented Generation (RAG) if retrieval filters are provided.
 * @author jsanca
 */
public final class CompletionRequest implements CompletionSpec {

    private final ModelConfig modelConfig = null; // todo
    private final String modelProviderKey = null;  //todo

    /** User instruction to the model. Keep it concise and specific. */
    @Size(min = 1, max = 4096)
    private final String prompt;
    /** Max number of retrieved chunks. */
    @Min(1)
    @Max(1000)
    private final Integer searchLimit;
    /** Offset for retrieved chunks (pagination). */
    @Min(0)
    private final Integer searchOffset;

    @Min(128)
    public final int responseLengthTokens = 0; // todo: this is not mapped

    // --- Localization / formatting ---
    /** Language ID or locale selector (implementation-specific). */
    private final Long language;
    /** Whether to stream tokens (SSE/chunked). */
    private final Boolean stream;
    /** Field to use as the primary text source (e.g., body, wysiwyg). */
    private final String fieldVar;


    // --- Optional: Retrieval filters (RAG) ---
    /** Site/host filter for retrieval. */
    private final String site;
    /** Content types to include in retrieval (e.g., Blog, Product). */
    private final List<String> contentType;

    private final String indexName = null; // todo this is not here

    /** Similarity operator to use when ranking retrieved chunks. */
    private final String operator; // "cosine" | "innerProduct" | "distance"
    /** Similarity threshold (0..1). Items below threshold are discarded. */
    private final Double threshold;

    // --- Generation parameters ---
    /** Provider/model identifier (e.g., "gpt-4o-mini", "gemini-pro", "llama3"). */
    private final String model;
    /** Temperature (0..2). Lower is more deterministic. */
    @Min(0)
    @Max(2)
    private final Float temperature;
    /** Max number of output tokens. */
    private final Integer maxTokens;

    /** Optional response format hints (e.g., JSON schema). */
    private final Map<String, Object> responseFormat;

    private CompletionRequest(Builder builder) {
        this.prompt = Objects.requireNonNull(builder.prompt);
        this.site = builder.site;
        this.contentType = builder.contentType;
        this.fieldVar = builder.fieldVar;
        this.searchLimit = builder.searchLimit;
        this.searchOffset = builder.searchOffset;
        this.operator = builder.operator;
        this.threshold = builder.threshold;
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.stream = builder.stream;
        this.language = builder.language;
        this.responseFormat = builder.responseFormat;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public String getModelProviderKey() {
        return modelProviderKey;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getSite() {
        return site;
    }

    public List<String> getContentType() {
        return contentType;
    }

    public String getFieldVar() {
        return fieldVar;
    }

    public Integer getSearchLimit() {
        return searchLimit;
    }

    public Integer getSearchOffset() {
        return searchOffset;
    }

    public String getOperator() {
        return operator;
    }

    public Double getThreshold() {
        return threshold;
    }

    public String getModel() {
        return model;
    }

    public Float getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Boolean getStream() {
        return stream;
    }

    public Long getLanguage() {
        return language;
    }


    public Map<String, Object> getResponseFormat() {
        return responseFormat;
    }

    @Override
    public String getEmbeddinModelProviderKey() {
        return ""; // todo:
    }

    @Override
    public String toString() {
        return "CompletionRequest{" +
                "prompt='" + prompt + '\'' +
                ", site='" + site + '\'' +
                ", contentType=" + contentType +
                ", fieldVar='" + fieldVar + '\'' +
                ", searchLimit=" + searchLimit +
                ", searchOffset=" + searchOffset +
                ", operator='" + operator + '\'' +
                ", threshold=" + threshold +
                ", model='" + model + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", stream=" + stream +
                ", language=" + language +
                ", responseFormat=" + responseFormat +
                '}';
    }

    /**
     * Creates a new Builder instance initialized with the values of the given CompletionRequest.
     * This is useful for creating a mutable copy of an immutable object.
     * @param request The object to copy.
     * @return A new Builder with the object's values.
     */
    public static Builder of(CompletionRequest request) {
        return builder()
                .prompt(request.getPrompt())
                .site(request.getSite())
                .contentType(request.getContentType())
                .fieldVar(request.getFieldVar())
                .searchLimit(request.getSearchLimit())
                .searchOffset(request.getSearchOffset())
                .operator(request.getOperator())
                .threshold(request.getThreshold())
                .model(request.getModel())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .stream(request.getStream())
                .language(request.getLanguage())
                .responseFormat(request.getResponseFormat());
    }


    public static Builder builder() {
        return new Builder();
    }

    public String getStreamingModelProviderKey() {
        return null; //todo:
    }

    // --- Builder class ---
    public static final class Builder {
        private String prompt;
        private String site;
        private List<String> contentType;
        private String fieldVar;
        private Integer searchLimit;
        private Integer searchOffset;
        private String operator;
        private Double threshold;
        private String model;
        private Float temperature;
        private Integer maxTokens;
        private Boolean stream;
        private Long language;
        private Map<String, Object> responseFormat;

        private Builder() {}

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder site(String site) {
            this.site = site;
            return this;
        }

        public Builder contentType(List<String> contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder fieldVar(String fieldVar) {
            this.fieldVar = fieldVar;
            return this;
        }

        public Builder searchLimit(Integer searchLimit) {
            this.searchLimit = searchLimit;
            return this;
        }

        public Builder searchOffset(Integer searchOffset) {
            this.searchOffset = searchOffset;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder threshold(Double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder language(Long language) {
            this.language = language;
            return this;
        }

        public Builder responseFormat(Map<String, Object> responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public CompletionRequest build() {
            return new CompletionRequest(this);
        }
    }
}
