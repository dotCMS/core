package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@link ModelProviderStrategy} implementation for Google Vertex AI.
 *
 * <p>Supports chat (streaming and non-streaming) via the Gemini model family.
 * Embeddings and image generation are not supported through this integration.
 *
 * <p>Authentication is resolved in the following order:
 * <ol>
 *   <li>If {@code credentialsJson} is set, uses the provided service account JSON directly.</li>
 *   <li>Otherwise, falls back to Application Default Credentials (ADC),
 *       suitable for GKE/Cloud Run workload identity.</li>
 * </ol>
 */
class VertexAiModelProviderStrategy implements ModelProviderStrategy {

    private static final String GCP_CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    @Override
    public String providerName() {
        return "vertex_ai";
    }

    @Override
    public ChatModel buildChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        if (config.timeout() != null) {
            Logger.warn(VertexAiModelProviderStrategy.class,
                    "timeout is not supported for Vertex AI and will be ignored");
        }
        if (config.credentialsJson() != null && !config.credentialsJson().isBlank()) {
            if (config.maxRetries() != null) {
                Logger.warn(VertexAiModelProviderStrategy.class,
                        "maxRetries is not supported when using credentialsJson auth for Vertex AI and will be ignored");
            }
            Logger.debug(VertexAiModelProviderStrategy.class, "building chat model for Vertex AI with credentialsJson auth");
            final VertexAI vertexAI = buildVertexAI(config);
            final GenerativeModel generativeModel = new GenerativeModel(config.model(), vertexAI);
            return new VertexAiGeminiChatModel(generativeModel, buildGenerationConfig(config));
        }
        Logger.debug(VertexAiModelProviderStrategy.class, "building chat model for Vertex AI with Application Default Credentials");
        final VertexAiGeminiChatModel.VertexAiGeminiChatModelBuilder builder =
                VertexAiGeminiChatModel.builder()
                        .project(config.projectId())
                        .location(config.location())
                        .modelName(config.model());
        if (config.maxRetries() != null) {
            builder.maxRetries(config.maxRetries());
        }
        if (config.temperature() != null) {
            builder.temperature(config.temperature().floatValue());
        }
        if (config.maxTokens() != null) {
            builder.maxOutputTokens(config.maxTokens());
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel buildStreamingChatModel(final ProviderConfig config, final String modelType) {
        validate(config, modelType);
        if (config.timeout() != null) {
            Logger.warn(VertexAiModelProviderStrategy.class,
                    "timeout is not supported for Vertex AI streaming providers and will be ignored");
        }
        if (config.maxRetries() != null) {
            Logger.warn(VertexAiModelProviderStrategy.class,
                    "maxRetries is not supported for Vertex AI streaming providers and will be ignored");
        }
        if (config.credentialsJson() != null && !config.credentialsJson().isBlank()) {
            Logger.debug(VertexAiModelProviderStrategy.class, "building streaming chat model for Vertex AI with credentialsJson auth");
            final VertexAI vertexAI = buildVertexAI(config);
            final GenerativeModel generativeModel = new GenerativeModel(config.model(), vertexAI);
            return new VertexAiGeminiStreamingChatModel(generativeModel, buildGenerationConfig(config));
        }
        Logger.debug(VertexAiModelProviderStrategy.class, "building streaming chat model for Vertex AI with Application Default Credentials");
        final VertexAiGeminiStreamingChatModel.VertexAiGeminiStreamingChatModelBuilder builder =
                VertexAiGeminiStreamingChatModel.builder()
                        .project(config.projectId())
                        .location(config.location())
                        .modelName(config.model());
        if (config.temperature() != null) {
            builder.temperature(config.temperature().floatValue());
        }
        if (config.maxTokens() != null) {
            builder.maxOutputTokens(config.maxTokens());
        }
        return builder.build();
    }

    @Override
    public EmbeddingModel buildEmbeddingModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "Embeddings are not supported for Vertex AI provider via LangChain4J");
    }

    @Override
    public ImageModel buildImageModel(final ProviderConfig config, final String modelType) {
        throw new UnsupportedOperationException(
                "Image generation is not supported for Vertex AI provider via LangChain4J");
    }

    private void validate(final ProviderConfig config, final String modelType) {
        ModelProviderStrategy.requireNonBlank(config.model(), "model", modelType);
        ModelProviderStrategy.requireNonBlank(config.projectId(), "projectId", modelType);
        ModelProviderStrategy.requireNonBlank(config.location(), "location", modelType);
    }

    private static VertexAI buildVertexAI(final ProviderConfig config) {
        final VertexAI.Builder builder = new VertexAI.Builder()
                .setProjectId(config.projectId())
                .setLocation(config.location());
        if (config.credentialsJson() != null && !config.credentialsJson().isBlank()) {
            Logger.debug(VertexAiModelProviderStrategy.class, "authenticating Vertex AI with credentialsJson");
            try {
                final GoogleCredentials credentials = GoogleCredentials
                        .fromStream(new ByteArrayInputStream(
                                config.credentialsJson().getBytes(StandardCharsets.UTF_8)))
                        .createScoped(GCP_CLOUD_PLATFORM_SCOPE);
                builder.setCredentials(credentials);
            } catch (final IOException e) {
                throw new IllegalArgumentException(
                        "vertex_ai: failed to parse credentialsJson", e);
            }
        } else {
            Logger.debug(VertexAiModelProviderStrategy.class, "authenticating Vertex AI with Application Default Credentials");
        }
        return builder.build();
    }

    private static GenerationConfig buildGenerationConfig(final ProviderConfig config) {
        final GenerationConfig.Builder builder = GenerationConfig.newBuilder();
        if (config.temperature() != null) {
            builder.setTemperature(config.temperature().floatValue());
        }
        if (config.maxTokens() != null) {
            builder.setMaxOutputTokens(config.maxTokens());
        }
        return builder.build();
    }

}
