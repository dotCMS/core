package com.dotcms.ai.v2.api;

import java.util.Map;
import java.util.List;
import java.util.Objects;

/**
 * Summarization request built on top of CompletionRequest.
 * Allows callers to hint style/length without handcrafting prompts.
 */
public final class SummarizeRequest implements CompletionSpec {

    // Attributes from CompletionRequest
    private final String prompt;
    private final String site;
    private final List<String> contentType;
    private final String fieldVar;
    private final Integer searchLimit;
    private final Integer searchOffset;
    private final String operator;
    private final Double threshold;
    private final String model;
    private final Float temperature;
    private final Integer maxTokens;
    private final Boolean stream;
    private final Long language;
    private final Map<String, Object> responseFormat;

    // Attributes specific to SummarizeRequest
    private final String style;
    private final Integer maxChars;
    private final Map<String, Object> options;

    private SummarizeRequest(Builder builder) {
        // Required field from the base class
        this.prompt = Objects.requireNonNull(builder.prompt);

        // All fields from CompletionRequest
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

        // Fields specific to this class
        this.style = builder.style;
        this.maxChars = builder.maxChars;
        this.options = builder.options;
    }

    /**
     * Creates a new Builder instance initialized with the values of the given SummarizeRequest.
     * This is useful for creating a mutable copy of an immutable object.
     * @param request The object to copy.
     * @return A new Builder with the object's values.
     */
    public static Builder of(SummarizeRequest request) {
        return builder()
                // Copying all fields from the parent class (CompletionSpec)
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
                .responseFormat(request.getResponseFormat())
                // Copying fields specific to SummarizeRequest
                .style(request.getStyle())
                .maxChars(request.getMaxChars())
                .options(request.getOptions());
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getters for all fields ---
    public String getPrompt() { return prompt; }
    public String getSite() { return site; }
    public List<String> getContentType() { return contentType; }
    public String getFieldVar() { return fieldVar; }
    public Integer getSearchLimit() { return searchLimit; }
    public Integer getSearchOffset() { return searchOffset; }
    public String getOperator() { return operator; }
    public Double getThreshold() { return threshold; }
    public String getModel() { return model; }
    public Float getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public Boolean getStream() { return stream; }
    public Long getLanguage() { return language; }
    public Map<String, Object> getResponseFormat() { return responseFormat; }

    @Override
    public String getEmbeddinModelProviderKey() {
        return ""; // todo:
    }

    public String getStyle() { return style; }
    public Integer getMaxChars() { return maxChars; }
    public Map<String, Object> getOptions() { return options; }

    // --- Builder class for SummarizeRequest ---
    public static final class Builder {
        // All fields from both classes
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
        private String style;
        private Integer maxChars;
        private Map<String, Object> options;

        private Builder() {}

        // Setters for CompletionRequest fields
        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder site(String site) { this.site = site; return this; }
        public Builder contentType(List<String> contentType) { this.contentType = contentType; return this; }
        public Builder fieldVar(String fieldVar) { this.fieldVar = fieldVar; return this; }
        public Builder searchLimit(Integer searchLimit) { this.searchLimit = searchLimit; return this; }
        public Builder searchOffset(Integer searchOffset) { this.searchOffset = searchOffset; return this; }
        public Builder operator(String operator) { this.operator = operator; return this; }
        public Builder threshold(Double threshold) { this.threshold = threshold; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder temperature(Float temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder stream(Boolean stream) { this.stream = stream; return this; }
        public Builder language(Long language) { this.language = language; return this; }
        public Builder responseFormat(Map<String, Object> responseFormat) { this.responseFormat = responseFormat; return this; }

        // Setters for SummarizeRequest fields
        public Builder style(String style) { this.style = style; return this; }
        public Builder maxChars(Integer maxChars) { this.maxChars = maxChars; return this; }
        public Builder options(Map<String, Object> options) { this.options = options; return this; }

        public SummarizeRequest build() {
            return new SummarizeRequest(this);
        }
    }
}
