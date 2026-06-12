package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class LangChain4jModelFactoryTest {

    /**
     * Given a null config,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(null));
    }

    /**
     * Given a valid OpenAI config,
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully.
     */
    @Test
    public void test_buildChatModel_openai_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(openAiConfig("gpt-4o-mini"));
        assertNotNull(model);
    }

    /**
     * Given an OpenAI config without a model,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai")
                .apiKey("test-key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given an OpenAI config without an apiKey,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_openai_missingApiKey_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai")
                .model("gpt-4o-mini")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given a valid Azure OpenAI config,
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully.
     */
    @Test
    public void test_buildChatModel_azureOpenai_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(azureOpenAiConfig("gpt-4o"));
        assertNotNull(model);
    }

    /**
     * Given an Azure OpenAI config without an apiKey,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_azureOpenai_missingApiKey_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-4o")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given an Azure OpenAI config without an endpoint,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_azureOpenai_missingEndpoint_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-4o")
                .apiKey("test-key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given a Vertex AI config without a projectId,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_vertexAi_missingProjectId_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .model("gemini-1.5-pro")
                .location("us-central1")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given a Vertex AI config without a location,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_vertexAi_missingLocation_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .model("gemini-1.5-pro")
                .projectId("my-gcp-project")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given a config with an unsupported provider name,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_bedrock_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(bedrockConfig("anthropic.claude-3-5-sonnet-20241022-v2:0"));
        assertNotNull(model);
    }

    @Test
    public void test_buildChatModel_bedrock_missingRegion_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("anthropic.claude-3-5-sonnet-20241022-v2:0")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    @Test
    public void test_buildChatModel_unknownProvider_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("unknown-provider")
                .model("some-model")
                .apiKey("key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given a null config,
     * When buildEmbeddingModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildEmbeddingModel_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildEmbeddingModel(null));
    }

    /**
     * Given a valid OpenAI config,
     * When buildEmbeddingModel is called,
     * Then an EmbeddingModel is returned successfully.
     */
    @Test
    public void test_buildEmbeddingModel_openai_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(openAiConfig("text-embedding-ada-002"));
        assertNotNull(model);
    }

    /**
     * Given a valid Azure OpenAI config,
     * When buildEmbeddingModel is called,
     * Then an EmbeddingModel is returned successfully.
     */
    @Test
    public void test_buildEmbeddingModel_azureOpenai_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(azureOpenAiConfig("text-embedding-ada-002"));
        assertNotNull(model);
    }

    /**
     * Given a Vertex AI config,
     * When buildEmbeddingModel is called,
     * Then an UnsupportedOperationException is thrown since embeddings are not supported for Vertex AI.
     */
    @Test
    public void test_buildEmbeddingModel_vertexAi_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> LangChain4jModelFactory.buildEmbeddingModel(vertexAiConfig("text-embedding-004")));
    }

    /**
     * Given a config with an unsupported provider name,
     * When buildEmbeddingModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildEmbeddingModel_bedrock_titan_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(bedrockConfig("amazon.titan-embed-text-v2:0"));
        assertNotNull(model);
    }

    @Test
    public void test_buildEmbeddingModel_bedrock_titan_withDimensions_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("amazon.titan-embed-text-v2:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .dimensions(1024)
                .build();
        assertNotNull(LangChain4jModelFactory.buildEmbeddingModel(config));
    }

    @Test
    public void test_buildEmbeddingModel_bedrock_cohere_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(bedrockConfig("cohere.embed-english-v3"));
        assertNotNull(model);
    }

    @Test
    public void test_buildEmbeddingModel_unknownProvider_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("unknown-provider")
                .model("some-model")
                .apiKey("key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildEmbeddingModel(config));
    }

    /**
     * Given a null config,
     * When buildImageModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildImageModel_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildImageModel(null));
    }

    /**
     * Given a valid OpenAI config,
     * When buildImageModel is called,
     * Then an ImageModel is returned successfully.
     */
    @Test
    public void test_buildImageModel_openai_returnsModel() {
        final ImageModel model = LangChain4jModelFactory.buildImageModel(openAiConfig("dall-e-3"));
        assertNotNull(model);
    }

    /**
     * Given a valid Azure OpenAI config,
     * When buildImageModel is called,
     * Then an ImageModel is returned successfully.
     */
    @Test
    public void test_buildImageModel_azureOpenai_returnsModel() {
        final ImageModel model = LangChain4jModelFactory.buildImageModel(azureOpenAiConfig("dall-e-3"));
        assertNotNull(model);
    }

    /**
     * Given a Vertex AI config,
     * When buildImageModel is called,
     * Then an UnsupportedOperationException is thrown since image generation is not supported for Vertex AI.
     */
    @Test
    public void test_buildImageModel_vertexAi_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> LangChain4jModelFactory.buildImageModel(vertexAiConfig("imagen-3.0")));
    }

    /**
     * Given a config with an unsupported provider name,
     * When buildImageModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildImageModel_bedrock_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> LangChain4jModelFactory.buildImageModel(bedrockConfig("stability.stable-diffusion-xl-v1")));
    }

    @Test
    public void test_buildImageModel_unknownProvider_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("unknown-provider")
                .model("some-model")
                .apiKey("key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildImageModel(config));
    }

    // ── Bedrock edge cases ────────────────────────────────────────────────────

    @Test
    public void test_buildChatModel_bedrock_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    // ── Vertex AI edge cases ──────────────────────────────────────────────────

    /**
     * Given a valid Vertex AI config using Application Default Credentials,
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully.
     */
    @Test
    public void test_buildChatModel_vertexAi_returnsModel() {
        assertNotNull(LangChain4jModelFactory.buildChatModel(vertexAiConfig("gemini-1.5-pro")));
    }

    /**
     * Given a Vertex AI config without a model,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_vertexAi_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .projectId("my-gcp-project")
                .location("us-central1")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    @Test
    public void test_buildEmbeddingModel_bedrock_unknownFamily_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> LangChain4jModelFactory.buildEmbeddingModel(bedrockConfig("meta.llama3-70b-instruct-v1:0")));
    }

    @Test
    public void test_buildEmbeddingModel_bedrock_cohereUppercase_routesToCohere() {
        assertNotNull(LangChain4jModelFactory.buildEmbeddingModel(bedrockConfig("Cohere.embed-english-v3")));
    }

    /**
     * Given a Vertex AI config with a valid credentialsJson,
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully.
     */
    @Test
    public void test_buildChatModel_vertexAi_validCredentialsJson_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .model("gemini-2.0-flash")
                .projectId("test-project")
                .location("us-central1")
                .credentialsJson(TEST_SERVICE_ACCOUNT_JSON)
                .build();
        assertNotNull(LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given a Vertex AI config with an invalid credentialsJson,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown with a message indicating the credentials could not be parsed.
     */
    @Test
    public void test_buildChatModel_vertexAi_invalidCredentialsJson_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .model("gemini-2.0-flash")
                .projectId("test-project")
                .location("us-central1")
                .credentialsJson("not-valid-json")
                .build();
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> LangChain4jModelFactory.buildChatModel(config));
        assertTrue(ex.getMessage().contains("failed to parse credentialsJson"));
    }

    /**
     * Given a Vertex AI config with a valid credentialsJson,
     * When buildStreamingChatModel is called,
     * Then a StreamingChatModel is returned successfully.
     */
    @Test
    public void test_buildStreamingChatModel_vertexAi_validCredentialsJson_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .model("gemini-2.0-flash")
                .projectId("test-project")
                .location("us-central1")
                .credentialsJson(TEST_SERVICE_ACCOUNT_JSON)
                .build();
        assertNotNull(LangChain4jModelFactory.buildStreamingChatModel(config));
    }

    // ── Azure edge cases ──────────────────────────────────────────────────────

    /**
     * Given an Azure OpenAI config with only a deploymentName (no model),
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully using the deploymentName.
     */
    @Test
    public void test_buildChatModel_azureOpenai_deploymentNameOnly_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .deploymentName("my-gpt4o-deployment")
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertNotNull(LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given an Azure OpenAI config with only a model (no deploymentName),
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully using the model as the deployment name.
     */
    @Test
    public void test_buildChatModel_azureOpenai_modelOnly_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-4o")
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertNotNull(LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given an Azure OpenAI config with a blank deploymentName and a model,
     * When buildChatModel is called,
     * Then a ChatModel is returned successfully falling back to the model value.
     */
    @Test
    public void test_buildChatModel_azureOpenai_blankDeploymentName_fallsBackToModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-4o")
                .deploymentName("")
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertNotNull(LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given an Azure OpenAI config with neither model nor deploymentName,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildChatModel_azureOpenai_missingBothModelAndDeploymentName_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    /**
     * Given an Azure OpenAI config with a dimensions parameter,
     * When buildEmbeddingModel is called,
     * Then an EmbeddingModel is returned successfully with the configured dimensions.
     */
    @Test
    public void test_buildEmbeddingModel_azureOpenai_withDimensions_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("text-embedding-3-small")
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .dimensions(512)
                .build();
        assertNotNull(LangChain4jModelFactory.buildEmbeddingModel(config));
    }

    // ── Azure Foundry endpoint auto-detection ─────────────────────────────────

    /**
     * Given an Azure OpenAI config with a Foundry endpoint (services.ai.azure.com),
     * When buildImageModel is called,
     * Then an ImageModel is returned using the plain OpenAI-style path (no isMicrosoftFoundry routing).
     */
    @Test
    public void test_buildImageModel_azureOpenai_foundryEndpoint_returnsModel() {
        assertNotNull(LangChain4jModelFactory.buildImageModel(azureFoundryConfig("gpt-image-2")));
    }

    /**
     * Given an Azure OpenAI Foundry config with optional size, timeout, and maxRetries,
     * When buildImageModel is called,
     * Then an ImageModel is returned successfully.
     */
    @Test
    public void test_buildImageModel_azureOpenai_foundryEndpoint_withOptionalParams_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-image-2")
                .apiKey("test-key")
                .endpoint("https://my-resource.services.ai.azure.com/openai/v1/")
                .size("1024x1024")
                .timeout(60)
                .maxRetries(2)
                .build();
        assertNotNull(LangChain4jModelFactory.buildImageModel(config));
    }

    /**
     * Given an Azure OpenAI Foundry config without model or deploymentName,
     * When buildImageModel is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_buildImageModel_azureOpenai_foundryEndpoint_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .apiKey("test-key")
                .endpoint("https://my-resource.services.ai.azure.com/openai/v1/")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildImageModel(config));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ProviderConfig openAiConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("openai")
                .model(model)
                .apiKey("test-key")
                .build();
    }

    private static ProviderConfig bedrockConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model(model)
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
    }

    private static ProviderConfig vertexAiConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("vertex_ai")
                .model(model)
                .projectId("my-gcp-project")
                .location("us-central1")
                .build();
    }

    /**
     * Fake service account JSON with a real-format RSA private key.
     * Not connected to any GCP project — for unit tests only.
     */
    private static final String TEST_SERVICE_ACCOUNT_JSON =
            "{\"type\":\"service_account\",\"project_id\":\"test-project\"," +
            "\"private_key_id\":\"key-id-001\"," +
            "\"private_key\":\"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDH5K/47TvSQi5f\\n5/vBRHyrk0YsuHiLaQaRNeFfIe7jA59BBI+EpYlYMHqxWyiUcHTYAAWgK25uBxGg\\nQNX6ANWQ4yk6cn/zu9jlBymk4pqdDyB/qDHd1RXgAM/gSCg5wjbh+TRG0BXwCY/I\\nPh4+pCtJc837Tq1iv1otBOmQd75u7uMiH7DRtyM1mZgy9ql18ykU8GerCf+iVMhz\\n+0tK/zaGpxkYjsX4PJ9qQ3+M4aLWycRMOwju4QpqAC3ntMH2PPESvNtHjtf96h0Q\\n0FJvSm4ymsa2D8Lp0+LBRS2Xp2drTnJ5dfV15CGnNksAAjSNKsHoXiopBltrg3FR\\nY/ouh0IpAgMBAAECggEAB0kW31UhMSmDmHADeWCyRQwf+oUKMrWUHVKICXA6jVhe\\nU2+zvedNJVNWA3URunBLV+yPR+5RTu4PxsYaLmudnRNkdyqBbpwd/IH5cval4TvS\\nCMkT8TSBILoS2YczvSAuoSeUr9CJfidRD+CwuEKCR+HyQ9qzH9Ze1mV3kFIG9TLM\\nA9PArYY/+njwffHpgC07up/imy4ZgSDwcKHpb1lzBFZ7xABwJm6ShKUuaQHy+DE1\\nGHsOIjgYaCE+5FrurJmXnPMbLYY/7e4BcZivwqEqeoK/O7/L2IJavG6UkLQR6J+U\\n4DrkUWRT/M20r/pZLp+BPFeW3Z8GOMVIMf1mG2n2YQKBgQDicuWd/4JVirefuMIc\\nTUlGfqwuc7G0HENL6XissTaATQb+dHxPXDRM8SmBzOca4MCqUh9v8ppe0UjAldlL\\npOFegVHl7xk+Cg4lFAkz19f/uYYEzIE+o/t9AHQcQIF75iachHpe9oKEyVluvtyQ\\nRakULUM4S7nRXDsBECgmj1FL4QKBgQDh+qK+DnWky/fg1Ll7Bf6Mve+H44aTHmIF\\nV39iOFI0U1ILBZRGlRiNEwJ8u0qbPMPnHakEApaVENRcoUCXeLPSR2Pl4o/IonsA\\nq8evIuOcGNdEQ42tCI9mDrLfICBSToLQT2+t/fPI+1CmbuFGISURbt6HlAe1txPt\\nNGcg0jR/SQKBgQCmkn31Sw5EflW0V+PxjahpAqUFRnVhV6jpHkR2Q6Ujm4WZh3Yp\\nqlmOf5dYs7EMRGd04hPd9Uc4mBo8D/5XdmGRcu8bhFW9HQhqneJnEE8C/SVoQkaQ\\nHW/Q85R1c73LtfCREaIyWIKJytLOc/cq6RMGlITt1EZLitycW5YtRx82AQKBgBs/\\nOywrVif+p+6l949L07tok18RIgLPIQZ/3E4deFvyx+uoSo6QH5uy9RPRCVfaZcyS\\nGPTw0vM6SkC4+K7K0PPDw9nODYQlgys7iyIk1MjXfe0vL//zDkdB8nNQzlTePoub\\naF3URvauxrSqneL00CFf+tjiEmPopTBupxif/9BxAoGAZashDiHM8ziv+4uM56ke\\nmTVD7skPRqc9zlhU0H41FiFi2bqvISA4n+OVe4kMrrjnQvNb5mJwN2coICMOUkhs\\npnrKJ9mG5iBa87elIae4SUhafXwi04l9tXphW4fbIIjlEGkqF9lm4LVLC3SLRP9D\\n0QqO4+57GNWD/yL4m9WJYr0=\\n-----END PRIVATE KEY-----\\n\"," +
            "\"client_email\":\"test-sa@test-project.iam.gserviceaccount.com\"," +
            "\"client_id\":\"123456789\"," +
            "\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\"," +
            "\"token_uri\":\"https://oauth2.googleapis.com/token\"}";
    private static ProviderConfig azureOpenAiConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model(model)
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .deploymentName(model)
                .apiVersion("2024-02-01")
                .build();
    }

    private static ProviderConfig azureFoundryConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model(model)
                .apiKey("test-key")
                .endpoint("https://my-resource.services.ai.azure.com/openai/v1/")
                .build();
    }

}
