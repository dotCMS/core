package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class LangChain4jModelFactoryTest {

    @Test
    public void test_buildChatModel_nullConfig_returnsNull() {
        assertNull(LangChain4jModelFactory.buildChatModel(null));
    }

    @Test
    public void test_buildChatModel_openai_returnsModel() {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(openAiConfig("gpt-4o-mini"));
        assertNotNull(model);
    }

    @Test
    public void test_buildChatModel_unknownProvider_throws() {
        final ProviderConfig config = new ProviderConfig();
        config.setProvider("unknown-provider");
        config.setModel("some-model");
        config.setApiKey("key");
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildChatModel(config));
    }

    @Test
    public void test_buildEmbeddingModel_nullConfig_returnsNull() {
        assertNull(LangChain4jModelFactory.buildEmbeddingModel(null));
    }

    @Test
    public void test_buildEmbeddingModel_openai_returnsModel() {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(openAiConfig("text-embedding-ada-002"));
        assertNotNull(model);
    }

    @Test
    public void test_buildEmbeddingModel_unknownProvider_throws() {
        final ProviderConfig config = new ProviderConfig();
        config.setProvider("unknown-provider");
        config.setModel("some-model");
        config.setApiKey("key");
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildEmbeddingModel(config));
    }

    @Test
    public void test_buildImageModel_nullConfig_returnsNull() {
        assertNull(LangChain4jModelFactory.buildImageModel(null));
    }

    @Test
    public void test_buildImageModel_openai_returnsModel() {
        final ImageModel model = LangChain4jModelFactory.buildImageModel(openAiConfig("dall-e-3"));
        assertNotNull(model);
    }

    @Test
    public void test_buildImageModel_unknownProvider_throws() {
        final ProviderConfig config = new ProviderConfig();
        config.setProvider("unknown-provider");
        config.setModel("some-model");
        config.setApiKey("key");
        assertThrows(IllegalArgumentException.class, () -> LangChain4jModelFactory.buildImageModel(config));
    }

    private static ProviderConfig openAiConfig(final String model) {
        final ProviderConfig config = new ProviderConfig();
        config.setProvider("openai");
        config.setModel(model);
        config.setApiKey("test-key");
        return config;
    }

}
