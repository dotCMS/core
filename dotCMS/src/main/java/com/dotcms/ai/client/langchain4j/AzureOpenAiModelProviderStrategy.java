package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import com.openai.azure.AzureOpenAIServiceVersion;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialImageModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class AzureOpenAiModelProviderStrategy implements ModelProviderStrategy {

    @Override
    public String providerName() {
        return "azure_openai";
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return Set.of(Capability.CHAT, Capability.EMBEDDINGS, Capability.IMAGE);
    }

    @Override
    public List<ProviderField> configFields(final Capability capability) {
        final List<ProviderField> common = List.of(
                ProviderField.required("apiKey", ProviderFieldType.SECRET),
                ProviderField.required("endpoint", ProviderFieldType.STRING),
                ProviderField.optionalUnless("model", ProviderFieldType.STRING, "deploymentName",
                        "Required if deploymentName is not set"),
                ProviderField.optionalUnless("deploymentName", ProviderFieldType.STRING, "model",
                        "Required if model is not set"),
                ProviderField.optional("apiVersion", ProviderFieldType.STRING, "e.g. 2024-02-01"));
        return switch (capability) {
            case CHAT -> concat(common,
                    ProviderField.optional("temperature", ProviderFieldType.NUMBER),
                    ProviderField.optional("maxTokens", ProviderFieldType.NUMBER),
                    ProviderField.optional("maxRetries", ProviderFieldType.NUMBER),
                    ProviderField.optional("timeout", ProviderFieldType.NUMBER));
            case EMBEDDINGS -> concat(common,
                    ProviderField.optional("dimensions", ProviderFieldType.NUMBER),
                    ProviderField.optional("maxRetries", ProviderFieldType.NUMBER),
                    ProviderField.optional("timeout", ProviderFieldType.NUMBER));
            case IMAGE -> concat(common,
                    ProviderField.optional("size", ProviderFieldType.STRING, "e.g. 1024x1024"),
                    ProviderField.optional("maxRetries", ProviderFieldType.NUMBER),
                    ProviderField.optional("timeout", ProviderFieldType.NUMBER));
        };
    }

    private static List<ProviderField> concat(final List<ProviderField> common, final ProviderField... extra) {
        final List<ProviderField> all = new ArrayList<>(common);
        all.addAll(List.of(extra));
        return List.copyOf(all);
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AzureOpenAiChatModel.Builder builder = AzureOpenAiChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) { builder.serviceVersion(config.apiVersion()); }
        if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        if (config.temperature() != null) { builder.temperature(config.temperature()); }
        applyTokenLimit(config, builder::maxTokens, builder::maxCompletionTokens);
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AzureOpenAiStreamingChatModel.Builder builder = AzureOpenAiStreamingChatModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) { builder.serviceVersion(config.apiVersion()); }
        if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        if (config.temperature() != null) { builder.temperature(config.temperature()); }
        applyTokenLimit(config, builder::maxTokens, builder::maxCompletionTokens);
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        final AzureOpenAiEmbeddingModel.Builder builder = AzureOpenAiEmbeddingModel.builder()
                .apiKey(config.apiKey())
                .endpoint(config.endpoint())
                .deploymentName(deploymentName(config));
        if (config.apiVersion() != null) { builder.serviceVersion(config.apiVersion()); }
        if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        if (config.dimensions() != null) { builder.dimensions(config.dimensions()); }
        return builder.build();
    }

    /**
     * Builds an image model using the official OpenAI Java SDK, automatically selecting the
     * correct routing based on the configured endpoint.
     *
     * <ul>
     *   <li><b>Azure AI Foundry</b> ({@code services.ai.azure.com}): uses a plain OpenAI-style
     *       client — no deployment-path routing, no {@code api-version} header. Compatible with
     *       models like {@code gpt-image-2} deployed via the Foundry catalog.</li>
     *   <li><b>Classic Azure OpenAI</b> ({@code openai.azure.com}): uses
     *       {@code isMicrosoftFoundry(true)}, which appends
     *       {@code /openai/deployments/{deploymentName}} to the base URL and injects the
     *       {@code api-version} query parameter. Supports models like {@code gpt-image-1}.</li>
     * </ul>
     *
     * <p>The legacy {@code AzureOpenAiImageModel} only supported dall-e-3, which was deprecated
     * by Azure in June 2025. Both paths here use {@code OpenAiOfficialImageModel} from the
     * official OpenAI Java SDK.
     */
    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        if (config.endpoint().contains("services.ai.azure.com")) {
            if (config.apiVersion() != null) {
                Logger.debug(AzureOpenAiModelProviderStrategy.class,
                        "apiVersion is not used for Azure AI Foundry endpoints and will be ignored");
            }
            final OpenAiOfficialImageModel.Builder builder = OpenAiOfficialImageModel.builder()
                    .baseUrl(config.endpoint())
                    .apiKey(config.apiKey())
                    .modelName(deploymentName(config));
            if (config.size() != null) { builder.size(config.size()); }
            if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
            if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
            return builder.build();
        }
        final OpenAiOfficialImageModel.Builder builder = OpenAiOfficialImageModel.builder()
                .isMicrosoftFoundry(true)
                .baseUrl(config.endpoint())
                .apiKey(config.apiKey())
                .microsoftFoundryDeploymentName(deploymentName(config))
                .modelName(deploymentName(config));
        if (config.apiVersion() != null) {
            builder.azureOpenAIServiceVersion(AzureOpenAIServiceVersion.Companion.fromString(config.apiVersion()));
        }
        if (config.size() != null) { builder.size(config.size()); }
        if (config.timeout() != null) { builder.timeout(Duration.ofSeconds(config.timeout())); }
        if (config.maxRetries() != null) { builder.maxRetries(config.maxRetries()); }
        return builder.build();
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.apiKey(), "apiKey", modelType);
        ModelProviderStrategy.requireNonBlank(config.endpoint(), "endpoint", modelType);
        if ((config.model() == null || config.model().isBlank())
                && (config.deploymentName() == null || config.deploymentName().isBlank())) {
            throw new IllegalArgumentException(
                    "providerConfig." + modelType + ": either 'model' or 'deploymentName' is required for azure_openai");
        }
    }

    private static String deploymentName(final ProviderConfig config) {
        return config.deploymentName() != null && !config.deploymentName().isBlank()
                ? config.deploymentName()
                : config.model();
    }

    private static boolean requiresCompletionTokens(final ProviderConfig config) {
        final String name = deploymentName(config) != null ? deploymentName(config) : "";
        return name.matches("o\\d+.*") || name.matches("gpt-([5-9]|\\d{2,}).*");
    }

    private static void applyTokenLimit(final ProviderConfig config,
                                        final Consumer<Integer> maxTokensFn,
                                        final Consumer<Integer> maxCompletionTokensFn) {
        final Integer tokens = config.maxCompletionTokens() != null
                ? config.maxCompletionTokens()
                : config.maxTokens();
        if (tokens == null) {
            return;
        }
        if (requiresCompletionTokens(config)) {
            maxCompletionTokensFn.accept(tokens);
        } else {
            maxTokensFn.accept(tokens);
        }
    }

}
