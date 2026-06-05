package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.time.Duration;
import java.util.function.Consumer;

class OpenAiModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.temperature() != null) builder.temperature(config.temperature());
        applyTokenLimit(config, builder::maxTokens, builder::maxCompletionTokens);
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl,
                ignored -> Logger.warn(OpenAiModelProviderStrategy.class,
                        "maxRetries is not supported by OpenAiStreamingChatModel and will be ignored"),
                builder::timeout);
        if (config.temperature() != null) builder.temperature(config.temperature());
        applyTokenLimit(config, builder::maxTokens, builder::maxCompletionTokens);
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.dimensions() != null) builder.dimensions(config.dimensions());
        return builder.build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.size() != null) builder.size(config.size());
        return builder.build();
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.model(), "model", modelType);
        ModelProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
    }

    private static void applyCommonConfig(final ProviderConfig config,
                                          final Consumer<String> baseUrlFn,
                                          final Consumer<Integer> retriesFn,
                                          final Consumer<Duration> timeoutFn) {
        if (config.endpoint() != null) baseUrlFn.accept(config.endpoint());
        if (config.maxRetries() != null) retriesFn.accept(config.maxRetries());
        if (config.timeout() != null) timeoutFn.accept(Duration.ofSeconds(config.timeout()));
    }

    /**
     * OpenAI reasoning models (o1, o3, o4-mini, etc.) and newer GPT models (gpt-5.x+) require
     * {@code max_completion_tokens} instead of {@code max_tokens}.
     */
    private static void applyTokenLimit(final ProviderConfig config,
                                        final Consumer<Integer> maxTokensFn,
                                        final Consumer<Integer> maxCompletionTokensFn) {
        final Integer tokens = config.maxCompletionTokens() != null
                ? config.maxCompletionTokens()
                : config.maxTokens();
        if (tokens == null) {
            return;
        }
        final String model = config.model() != null ? config.model() : "";
        if (model.matches("o\\d+.*") || model.matches("gpt-([5-9]|\\d{2,}).*")) {
            maxCompletionTokensFn.accept(tokens);
        } else {
            maxTokensFn.accept(tokens);
        }
    }

}
