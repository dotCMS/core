package com.dotcms.ai.api.provider.vertex;

import com.dotcms.ai.api.provider.ChatModelProvider;
import com.dotcms.ai.config.AiModelConfig;
import com.dotcms.util.ConversionUtils;
import com.google.auth.oauth2.GoogleCredentials;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Google Vertex AI Chat Model Provider implementation.
 * Supports models hosted in Google Cloud Vertex AI (e.g., Gemini, PaLM).
 * @author jsanca
 */
public class VertexChatModelProviderImpl implements ChatModelProvider {

    @Override
    public ChatModel create(final AiModelConfig config) {

        final String projectId   = config.getOrDefault("projectId", "");
        final String location    = config.getOrDefault("location", "us-central1");
        final String credentialsJsonPath = config.getOrDefault("location", "");
        final String model       = config.getOrDefault(AiModelConfig.MODEL, "gemini-1.5-flash");
        final Double temperature = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"), 0.3);
        final long timeoutMs     = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"), 30000L);
        final int maxOutput      = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, "2048"), 2048);
        final Double topP = ConversionUtils.toDouble(
                config.getOrDefault("topP", "0.9"), 0.9);
        final Integer topK = ConversionUtils.toInt(
                config.getOrDefault("topK", "40"), 40);

        GoogleCredentials credentials = null;
        if (!credentialsJsonPath.isEmpty() && Paths.get(credentialsJsonPath).toFile().exists()) {
            try {
                credentials = GoogleCredentials.fromStream(Files.newInputStream(Paths.get(credentialsJsonPath))); // this already close the input stream
            } catch (Exception e) {
                throw new RuntimeException("Error on loading the vertex credential on path: " + credentialsJsonPath, e);
            }
        }

        return VertexAiGeminiChatModel.builder()
                .project(projectId)
                .location(location)
                .modelName(model)
                .credentials(credentials)
                .temperature(temperature.floatValue())
                .topP(topP.floatValue())
                .topK(topK)
                .maxOutputTokens(maxOutput)
                .build();
    }

    @Override
    public StreamingChatModel createStreaming(final AiModelConfig config) {

        final String projectId   = config.getOrDefault("projectId", "");
        final String location    = config.getOrDefault("location", "us-central1");
        final String credentialsJsonPath = config.getOrDefault("location", "");
        final String model       = config.getOrDefault(AiModelConfig.MODEL, "gemini-1.5-flash");
        final Double temperature = ConversionUtils.toDouble(
                config.getOrDefault(AiModelConfig.TEMPERATURE, "0.3"), 0.3);
        final long timeoutMs     = ConversionUtils.toLong(
                config.getOrDefault(AiModelConfig.TIMEOUT_MS, "30000"), 30000L);
        final int maxOutput      = ConversionUtils.toInt(
                config.getOrDefault(AiModelConfig.MAX_OUTPUT_TOKENS, "2048"), 2048);
        final Double topP = ConversionUtils.toDouble(
                config.getOrDefault("topP", "0.9"), 0.9);
        final Integer topK = ConversionUtils.toInt(
                config.getOrDefault("topK", "40"), 40);

        GoogleCredentials credentials = null;
        if (!credentialsJsonPath.isEmpty() && Paths.get(credentialsJsonPath).toFile().exists()) {
            try {
                credentials = GoogleCredentials.fromStream(Files.newInputStream(Paths.get(credentialsJsonPath))); // this already close the input stream
            } catch (Exception e) {
                throw new RuntimeException("Error on loading the vertex credential on path: " + credentialsJsonPath, e);
            }
        }

        return VertexAiGeminiStreamingChatModel.builder()
                .project(projectId)
                .location(location)
                .modelName(model)
                .credentials(credentials)
                .temperature(temperature.floatValue())
                .topP(topP.floatValue())
                .topK(topK)
                .maxOutputTokens(maxOutput)
                .build();
    }
}
