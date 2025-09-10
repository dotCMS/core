package com.dotcms.ai.v2.api.embeddings.retrieval;

import com.dotcms.ai.v2.api.SimilarityOperator;

import java.util.List;
import java.util.Objects;

/**
 * Query to retrieve information from any RAG approach
 * @author jsanca
 */
public final class RetrievalQuery {

    private final String site;
    private final List<String> contentTypes;
    private final String languageId;
    private final String fieldVar;
    private final String prompt;
    private final int limit;
    private final int offset;
    private final double threshold;
    private final SimilarityOperator operator;
    private final String userId;

    private RetrievalQuery(Builder builder) {
        this.site = builder.site;
        this.contentTypes = builder.contentTypes;
        this.languageId = builder.languageId;
        this.fieldVar = builder.fieldVar;
        this.prompt = Objects.requireNonNull(builder.prompt);
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.threshold = builder.threshold;
        this.operator = builder.operator;
        this.userId = builder.userId;
    }

    // Getters for all fields
    public String getSite() {
        return site;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public String getLanguageId() {
        return languageId;
    }

    public String getFieldVar() {
        return fieldVar;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public double getThreshold() {
        return threshold;
    }

    public SimilarityOperator getOperator() {
        return operator;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "RetrievalQuery{" +
                "site='" + site + '\'' +
                ", contentTypes=" + contentTypes +
                ", languageId='" + languageId + '\'' +
                ", fieldVar='" + fieldVar + '\'' +
                ", prompt='" + prompt + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", threshold=" + threshold +
                ", operator=" + operator +
                ", userId='" + userId + '\'' +
                '}';
    }

    // Static builder methods
    public static Builder builder() {
        return new Builder();
    }

    public static Builder of(RetrievalQuery query) {
        return builder()
                .site(query.getSite())
                .contentTypes(query.getContentTypes())
                .languageId(query.getLanguageId())
                .fieldVar(query.getFieldVar())
                .prompt(query.getPrompt())
                .limit(query.getLimit())
                .offset(query.getOffset())
                .threshold(query.getThreshold())
                .operator(query.getOperator())
                .userId(query.getUserId());
    }

    // The Builder static nested class
    public static final class Builder {
        private String site;
        private List<String> contentTypes;
        private String languageId;
        private String fieldVar;
        private String prompt;
        private int limit;
        private int offset;
        private double threshold;
        private SimilarityOperator operator;
        private String userId;

        private Builder() {
            // Default values to provide a consistent state
            this.limit = 10;
            this.offset = 0;
            this.threshold = 0.7;
            this.operator = SimilarityOperator.COSINE;
        }

        public Builder site(String site) {
            this.site = site;
            return this;
        }

        public Builder contentTypes(List<String> contentTypes) {
            this.contentTypes = contentTypes;
            return this;
        }

        public Builder languageId(String languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder fieldVar(String fieldVar) {
            this.fieldVar = fieldVar;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * minimum score threshold that a result must meet to be considered relevant.
         *
         * examples:
         * Cosine similarity: Values close to 1.0 = very similar, close to 0 = slightly similar.
         *
         * Inner product: Large positive values = very similar.
         *
         * Euclidean distance: Small values = very similar (here we sometimes transform this to a score)
         * @param threshold
         * @return
         */
        public Builder threshold(double threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder operator(SimilarityOperator operator) {
            this.operator = operator;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public RetrievalQuery build() {
            return new RetrievalQuery(this);
        }
    }
}
