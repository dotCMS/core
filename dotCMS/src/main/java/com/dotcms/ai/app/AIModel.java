package com.dotcms.ai.app;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AIModel {

    private final String id;
    private final List<String> names;
    private final int tokensPerMinute;
    private final int apiPerMinute;
    private final int maxTokens;
    private final boolean isCompletion;
    private final AtomicInteger current;

    private AIModel(final String id,
                    final List<String> names,
                    final int tokensPerMinute,
                    final int apiPerMinute,
                    final int maxTokens,
                    final boolean isCompletion) {
        if (CollectionUtils.isEmpty(names)) {
            throw new IllegalArgumentException("Names cannot be empty");
        }

        this.id = id;
        this.names = names;
        this.tokensPerMinute = tokensPerMinute;
        this.apiPerMinute = apiPerMinute;
        this.maxTokens = maxTokens;
        this.isCompletion = isCompletion;
        current = new AtomicInteger(0);
    }

    public String getId() {
        return id;
    }

    public List<String> getNames() {
        return names;
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

    public int getCurrent() {
        return current.get();
    }

    public void setCurrent(final int current) {
        this.current.set(current);
    }

    public long minIntervalBetweenCalls() {
        return 60000 / apiPerMinute;
    }

    @Override
    public String toString() {
        return "AIModel{" +
                "name='" + names + '\'' +
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

        private String id;
        private List<String> names;
        private int tokensPerMinute;
        private int apiPerMinute;
        private int maxTokens;
        private boolean isCompletion;

        private Builder() {
        }

        public Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Builder withNames(final List<String> names) {
            this.names = names;
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
            return new AIModel(id, names, tokensPerMinute, apiPerMinute, maxTokens, isCompletion);
        }

    }

}
