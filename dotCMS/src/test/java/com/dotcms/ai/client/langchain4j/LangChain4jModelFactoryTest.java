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
    public void test_buildChatModel_azureOpenai_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(azureOpenAiConfig("gpt-4o"));
        assertNotNull(model);
    }

    @Test
    public void test_buildChatModel_bedrock_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(bedrockConfig("anthropic.claude-3-5-sonnet-20241022-v2:0"));
        assertNotNull(model);
    }

    @Test
    public void test_buildChatModel_vertexAi_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(vertexAiConfig("gemini-1.5-pro"));
        assertNotNull(model);
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
    public void test_buildEmbeddingModel_bedrock_titan_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(bedrockConfig("amazon.titan-embed-text-v2:0"));
        assertNotNull(model);
    }

    @Test
    public void test_buildEmbeddingModel_bedrock_cohere_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(bedrockConfig("cohere.embed-english-v3"));
        assertNotNull(model);
    }

    @Test
    public void test_buildEmbeddingModel_vertexAi_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> LangChain4jModelFactory.buildEmbeddingModel(vertexAiConfig("text-embedding-004")));
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
    public void test_buildImageModel_bedrock_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> LangChain4jModelFactory.buildImageModel(bedrockConfig("stability.stable-diffusion-xl-v1")));
    }

    @Test
    public void test_buildImageModel_vertexAi_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> LangChain4jModelFactory.buildImageModel(vertexAiConfig("imagen-3.0")));
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

    private static ProviderConfig openAiConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("openai")
                .model(model)
                .apiKey("test-key")
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

    private static ProviderConfig bedrockConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model(model)
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
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
