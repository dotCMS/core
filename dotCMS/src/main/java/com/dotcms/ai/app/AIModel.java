package com.dotcms.ai.app;

import com.dotcms.ai.domain.Model;
import com.dotcms.ai.exception.DotAIModelNotFoundException;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents an AI model with various attributes such as type, names, tokens per minute,
 * API calls per minute, maximum tokens, and completion status. This class provides methods
 * to manage the current model, decommission status, and calculate the minimum interval
 * between API calls. It also includes a builder for creating instances of AIModel.
 *
 * @author vico
 */
public class AIModel {

    public static final int NOOP_INDEX = -1;
    public static final AIModel NOOP_MODEL = AIModel.builder()
            .withType(AIModelType.UNKNOWN)
            .withModelNames(List.of())
            .build();

    private final AIModelType type;
    private final List<Model> models;
    private final int tokensPerMinute;
    private final int apiPerMinute;
    private final int maxTokens;
    private final boolean isCompletion;
    private final AtomicInteger currentModelIndex;

    private AIModel(final Builder builder) {
        DotPreconditions.checkNotNull(builder.type, "type cannot be null");
        this.type = builder.type;
        this.models = builder.models;
        this.tokensPerMinute = builder.tokensPerMinute;
        this.apiPerMinute = builder.apiPerMinute;
        this.maxTokens = builder.maxTokens;
        this.isCompletion = builder.isCompletion;
        currentModelIndex = new AtomicInteger(this.models.isEmpty() ? NOOP_INDEX  : 0);
    }

    public AIModelType getType() {
        return type;
    }

    public List<Model> getModels() {
        return models;
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

    public int getCurrentModelIndex() {
        return currentModelIndex.get();
    }

    public void setCurrentModelIndex(final int currentModelIndex) {
        this.currentModelIndex.set(currentModelIndex);
    }

    public boolean isOperational() {
        return this != NOOP_MODEL && models.stream().anyMatch(Model::isOperational);
    }

    public Model getCurrent() {
        final int currentIndex = currentModelIndex.get();
        if (!isCurrentValid(currentIndex)) {
            logInvalidModelMessage();
            return null;
        }
        return models.get(currentIndex);
    }

    public String getCurrentModel() {
        return Optional.ofNullable(getCurrent()).map(Model::getName).orElse(null);
    }

    public Model getModel(final String modelName) {
        final String normalized = modelName.trim().toLowerCase();
        return models.stream()
                .filter(model -> normalized.equals(model.getName()))
                .findFirst()
                .orElseThrow(() -> new DotAIModelNotFoundException(String.format("Model [%s] not found", modelName)));
    }

    public void repairCurrentIndexIfNeeded() {
        if (getCurrentModelIndex() != NOOP_INDEX) {
            return;
        }

        setCurrentModelIndex(
                getModels()
                        .stream()
                        .filter(Model::isOperational).findFirst().map(Model::getIndex)
                        .orElse(NOOP_INDEX));
    }

    public long minIntervalBetweenCalls() {
        return 60000 / apiPerMinute;
    }

    @Override
    public String toString() {
        return "AIModel{" +
                "type=" + type +
                ", models='" + models + '\'' +
                ", tokensPerMinute=" + tokensPerMinute +
                ", apiPerMinute=" + apiPerMinute +
                ", maxTokens=" + maxTokens +
                ", isCompletion=" + isCompletion +
                ", currentModelIndex=" + currentModelIndex.get() +
                '}';
    }

    private boolean isCurrentValid(final int current) {
        return !models.isEmpty() && current >= 0 && current < models.size();
    }

    private void logInvalidModelMessage() {
        Logger.debug(getClass(), String.format("Current model index must be between 0 and %d", models.size()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private AIModelType type;
        private List<Model> models;
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

        public Builder withModels(final List<Model> models) {
            this.models = Optional.ofNullable(models).orElse(List.of());
            return this;
        }

        public Builder withModelNames(final List<String> names) {
            return withModels(
                    Optional.ofNullable(names)
                            .map(modelNames -> IntStream.range(0, modelNames.size())
                                    .mapToObj(index -> Model.builder()
                                            .withName(modelNames.get(index))
                                            .withIndex(index)
                                            .build())
                                    .collect(Collectors.toList()))
                            .orElse(List.of()));
        }

        public Builder withModelNames(final String... names) {
            return withModelNames(List.of(names));
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
            return new AIModel(this);
        }

    }

}
