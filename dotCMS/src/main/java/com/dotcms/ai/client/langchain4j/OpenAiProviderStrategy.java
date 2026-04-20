package com.dotcms.ai.client.langchain4j;

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

/**
 * {@link ProviderStrategy} for OpenAI and OpenAI-compatible providers
 * (e.g. OpenRouter, any custom endpoint that mirrors the OpenAI API).
 */
public class OpenAiProviderStrategy implements ProviderStrategy {

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public void validate(final ProviderConfig config, final String modelType) {
        ProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config) {
        final OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxCompletionTokens() != null) {
            builder.maxCompletionTokens(config.maxCompletionTokens());
        } else if (config.maxTokens() != null) {
            builder.maxTokens(config.maxTokens());
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config) {
        final OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, ignored -> {}, builder::timeout);
        if (config.temperature() != null) builder.temperature(config.temperature());
        if (config.maxCompletionTokens() != null) {
            builder.maxCompletionTokens(config.maxCompletionTokens());
        } else if (config.maxTokens() != null) {
            builder.maxTokens(config.maxTokens());
        }
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config) {
        final OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.dimensions() != null) builder.dimensions(config.dimensions());
        return builder.build();
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config) {
        final OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .apiKey(config.apiKey())
                .modelName(config.model());
        applyCommonConfig(config, builder::baseUrl, builder::maxRetries, builder::timeout);
        if (config.size() != null) builder.size(config.size());
        return builder.build();
    }

    private static void applyCommonConfig(final ProviderConfig config,
                                          final Consumer<String> baseUrlFn,
                                          final Consumer<Integer> retriesFn,
                                          final Consumer<Duration> timeoutFn) {
        if (config.endpoint() != null) baseUrlFn.accept(config.endpoint());
        if (config.maxRetries() != null) retriesFn.accept(config.maxRetries());
        if (config.timeout() != null) timeoutFn.accept(Duration.ofSeconds(config.timeout()));
    }

}
