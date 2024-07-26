package com.dotcms.ai.app;

import com.dotcms.util.DotPreconditions;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AIModel {

    private final AIModelType type;
    private final List<String> names;
    private final int tokensPerMinute;
    private final int apiPerMinute;
    private final int maxTokens;
    private final boolean isCompletion;
    private final AtomicInteger current;
    private final AtomicBoolean decommissioned;

    private AIModel(final AIModelType type,
                    final List<String> names,
                    final int tokensPerMinute,
                    final int apiPerMinute,
                    final int maxTokens,
                    final boolean isCompletion) {
        DotPreconditions.checkNotNull(type, "type cannot be null");
        this.type = type;
        this.names = Optional.ofNullable(names).orElse(List.of());
        this.tokensPerMinute = tokensPerMinute;
        this.apiPerMinute = apiPerMinute;
        this.maxTokens = maxTokens;
        this.isCompletion = isCompletion;
        current = new AtomicInteger(this.names.isEmpty() ? -1  : 0);
        decommissioned = new AtomicBoolean(false);
    }

    public AIModelType getType() {
        return type;
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
        if (!isCurrentValid(current)) {
            logInvalidModelMessage();
            return;
        }
        this.current.set(current);
    }

    public boolean isDecommissioned() {
        return decommissioned.get();
    }

    public void setDecommissioned(final boolean decommissioned) {
        this.decommissioned.set(decommissioned);
    }

    public String getCurrentModel() {
        final int currentIndex = this.current.get();
        if (!isCurrentValid(currentIndex)) {
            logInvalidModelMessage();
            return null;
        }
        return names.get(currentIndex);
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

    private boolean isCurrentValid(final int current) {
        return !names.isEmpty() && current >= 0 && current < names.size();
    }

    private void logInvalidModelMessage() {
        Logger.debug(getClass(), String.format("Current model index must be between 0 and %d", names.size()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private AIModelType type;
        private List<String> names;
        private int tokensPerMinute;
        private int apiPerMinute;
        private int maxTokens;
        private boolean isCompletion;

        private Builder() {
        }

        public Builder withType(final AIModelType type) {
            this.type = type;
            return this;
        }

        public Builder withNames(final List<String> names) {
            this.names = names;
            return this;
        }

        public Builder withNames(final String... names) {
            return withNames(List.of(names));
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
            return new AIModel(type, names, tokensPerMinute, apiPerMinute, maxTokens, isCompletion);
        }

    }

}
