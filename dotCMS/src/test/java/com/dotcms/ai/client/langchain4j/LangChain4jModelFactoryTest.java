package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class LangChain4jModelFactoryTest {

    @Test
    public void test_buildChatModel_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(null));
    }

    @Test
    public void test_buildChatModel_openai_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(openAiConfig("gpt-4o-mini"));
        assertNotNull(model);
    }

    @Test
    public void test_buildChatModel_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai")
                .apiKey("test-key")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    @Test
    public void test_buildChatModel_openai_missingApiKey_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("openai")
                .model("gpt-4o-mini")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    @Test
    public void test_buildChatModel_azureOpenai_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(azureOpenAiConfig("gpt-4o"));
        assertNotNull(model);
    }

    @Test
    public void test_buildChatModel_azureOpenai_missingApiKey_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-4o")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    @Test
    public void test_buildChatModel_azureOpenai_missingEndpoint_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .model("gpt-4o")
                .apiKey("test-key")
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

    @Test
    public void test_buildEmbeddingModel_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildEmbeddingModel(null));
    }

    @Test
    public void test_buildEmbeddingModel_openai_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(openAiConfig("text-embedding-ada-002"));
        assertNotNull(model);
    }

    @Test
    public void test_buildEmbeddingModel_azureOpenai_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(azureOpenAiConfig("text-embedding-ada-002"));
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

    @Test
    public void test_buildImageModel_nullConfig_throws() {
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildImageModel(null));
    }

    @Test
    public void test_buildImageModel_openai_returnsModel() {
        final ImageModel model = LangChain4jModelFactory.buildImageModel(openAiConfig("dall-e-3"));
        assertNotNull(model);
    }

    @Test
    public void test_buildImageModel_azureOpenai_returnsModel() {
        final ImageModel model = LangChain4jModelFactory.buildImageModel(azureOpenAiConfig("dall-e-3"));
        assertNotNull(model);
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

    // ── Azure edge cases ──────────────────────────────────────────────────────

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

    @Test
    public void test_buildChatModel_azureOpenai_missingBothModelAndDeploymentName_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .apiKey("test-key")
                .endpoint("https://my-company.openai.azure.com/")
                .build();
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ProviderConfig openAiConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("openai")
                .model(model)
                .apiKey("test-key")
                .build();
    }

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

}
