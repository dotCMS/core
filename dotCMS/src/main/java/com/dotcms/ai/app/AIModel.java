package com.dotcms.ai.app;

import java.util.Objects;

public class AIModel {

    private final String name;
    private final int tokensPerMinute;
    private final int apiPerMinute;
    private final int maxTokens;
    private final boolean isCompletion;

    public AIModel(final String name,
                   final int tokensPerMinute,
                   final int apiPerMinute,
                   final int maxTokens,
                   final boolean isCompletion) {
        this.name = name;
        this.tokensPerMinute = tokensPerMinute;
        this.apiPerMinute = apiPerMinute;
        this.maxTokens = maxTokens;
        this.isCompletion = isCompletion;
    }

    public String getName() {
        return name;
    }

    public int getTokensPerMinute() {
        return tokensPerMinute;
    }

    public int getApiPerMinute() {
        return apiPerMinute;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public boolean isCompletion() {
        return isCompletion;
    }

    public long minIntervalBetweenCalls() {
        return 60000 / apiPerMinute;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AIModel aiModel = (AIModel) o;
        return Objects.equals(name, aiModel.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "AIModel{" +
                "name='" + name + '\'' +
                ", tokensPerMinute=" + tokensPerMinute +
                ", apiPerMinute=" + apiPerMinute +
                ", maxTokens=" + maxTokens +
                ", isCompletion=" + isCompletion +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;
        private int tokensPerMinute;
        private int apiPerMinute;
        private int maxTokens;
        private boolean isCompletion;

        private Builder() {
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withTokensPerMinute(final int tokensPerMinute) {
            this.tokensPerMinute = tokensPerMinute;
            return this;
        }

        public Builder withApiPerMinute(final int apiPerMinute) {
            this.apiPerMinute = apiPerMinute;
            return this;
        }

        public Builder withMaxTokens(final int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder withIsCompletion(final boolean isCompletion) {
            this.isCompletion = isCompletion;
            return this;
        }

        public AIModel build() {
            return new AIModel(name, tokensPerMinute, apiPerMinute, maxTokens, isCompletion);
        }

    }

}
