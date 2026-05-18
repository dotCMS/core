package com.dotcms.ai.client.langchain4j;

import com.dotcms.ai.AiKeys;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.client.JSONObjectAIRequest;
import com.dotcms.ai.exception.DotAIAppConfigDisabledException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LangChain4jAIClientTest {

    @Test
    public void test_toMessages_null_returnsEmptyList() {
        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(null);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void test_toMessages_systemRole_producesSystemMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "system", AiKeys.CONTENT, "You are helpful."));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertEquals("You are helpful.", ((SystemMessage) messages.get(0)).text());
    }

    @Test
    public void test_toMessages_assistantRole_producesAiMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "assistant", AiKeys.CONTENT, "I can help."));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof AiMessage);
        assertEquals("I can help.", ((AiMessage) messages.get(0)).text());
    }

    @Test
    public void test_toMessages_userRole_producesUserMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "user", AiKeys.CONTENT, "Hello!"));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
        assertEquals("Hello!", ((UserMessage) messages.get(0)).singleText());
    }

    @Test
    public void test_toMessages_unknownRole_defaultsToUserMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "unknown-role", AiKeys.CONTENT, "Some text"));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
    }

    @Test
    public void test_toMessages_multipleRoles_preservesOrder() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "system", AiKeys.CONTENT, "System prompt"));
        array.put(Map.of(AiKeys.ROLE, "user", AiKeys.CONTENT, "User question"));
        array.put(Map.of(AiKeys.ROLE, "assistant", AiKeys.CONTENT, "Assistant reply"));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(3, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertTrue(messages.get(1) instanceof UserMessage);
        assertTrue(messages.get(2) instanceof AiMessage);
    }

    @Test
    public void test_toChatResponseJson_correctStructure() {
        final AiMessage aiMessage = new AiMessage("Test response content");
        final ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(aiMessage);
        when(response.modelName()).thenReturn("gpt-4o-mini");

        final JSONObject json = new JSONObject(LangChain4jAIClient.toChatResponseJson(response));
        final JSONObject message = json.getJSONArray("choices").getJSONObject(0).getJSONObject(AiKeys.MESSAGE);

        assertEquals("assistant", message.getString(AiKeys.ROLE));
        assertEquals("Test response content", message.getString(AiKeys.CONTENT));
        assertEquals("gpt-4o-mini", json.getString(AiKeys.MODEL));
    }

    @Test
    public void test_toEmbeddingResponseJson_valuesStoredAsDoubles() {
        final Embedding embedding = Embedding.from(new float[]{0.1f, -0.2f, 0.3f});

        final JSONObject json = new JSONObject(LangChain4jAIClient.toEmbeddingResponseJson(embedding));
        final JSONArray embeddingArray = json.getJSONArray(AiKeys.DATA)
                .getJSONObject(0)
                .getJSONArray(AiKeys.EMBEDDING);

        assertEquals(3, embeddingArray.length());
        assertTrue(embeddingArray.get(0) instanceof Double);
        assertEquals(0.1, (Double) embeddingArray.get(0), 0.0001);
        assertEquals(-0.2, (Double) embeddingArray.get(1), 0.0001);
        assertEquals(0.3, (Double) embeddingArray.get(2), 0.0001);
    }

    @Test
    public void test_toImageResponseJson_containsUrl() throws Exception {
        final Image image = Image.builder().url(new URI("https://example.com/image.png")).build();

        final JSONObject json = new JSONObject(LangChain4jAIClient.toImageResponseJson(image));
        final String url = json.getJSONArray(AiKeys.DATA).getJSONObject(0).getString(AiKeys.URL);

        assertEquals("https://example.com/image.png", url);
    }

    @Test
    public void test_toImageResponseJson_nullUrl_returnsEmptyString() {
        final Image image = Image.builder().build();

        final JSONObject json = new JSONObject(LangChain4jAIClient.toImageResponseJson(image));
        final String url = json.getJSONArray(AiKeys.DATA).getJSONObject(0).getString(AiKeys.URL);

        assertEquals("", url);
    }

    // -------------------------------------------------------------------------
    // executeWithFallback — multi-model fallback behaviour
    // -------------------------------------------------------------------------

    private static ProviderConfig configWithModels(final String models) {
        return ImmutableProviderConfig.builder()
                .provider("openai")
                .apiKey("sk-test")
                .model(models)
                .build();
    }

    private static Cache<String, String> freshCache() {
        return CacheBuilder.newBuilder().build();
    }

    @Test
    public void test_executeWithFallback_initFailure_fallsBackToNextModel() {
        final ProviderConfig config = configWithModels("bad-model,good-model");
        final Cache<String, String> cache = freshCache();

        // builder throws for "bad-model", succeeds for "good-model"
        final String result = LangChain4jAIClient.get().executeWithFallback(
                "test", "chat", config, cache,
                cfg -> {
                    if ("bad-model".equals(cfg.model())) {
                        throw new IllegalStateException("simulated init failure");
                    }
                    return "good-instance";
                },
                model -> "response-from-" + model);

        assertEquals("response-from-good-instance", result);
    }

    @Test
    public void test_executeWithFallback_runtimeFailure_fallsBackToNextModel() {
        final ProviderConfig config = configWithModels("bad-model,good-model");
        final Cache<String, String> cache = freshCache();

        // builder always succeeds, executor throws on the first model
        final AtomicInteger executorCalls = new AtomicInteger();
        final String result = LangChain4jAIClient.get().executeWithFallback(
                "test", "chat", config, cache,
                cfg -> cfg.model(),   // model instance = model name string
                model -> {
                    if (executorCalls.getAndIncrement() == 0) {
                        throw new RuntimeException("simulated runtime failure");
                    }
                    return "response-from-" + model;
                });

        assertEquals("response-from-good-model", result);
    }

    @Test
    public void test_executeWithFallback_allModelsFail_rethrowsLastException() {
        final ProviderConfig config = configWithModels("model-a,model-b");
        final Cache<String, String> cache = freshCache();
        final RuntimeException secondException = new RuntimeException("model-b failed");

        final AtomicInteger callCount = new AtomicInteger();
        final RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> LangChain4jAIClient.get().executeWithFallback(
                        "test", "chat", config, cache,
                        cfg -> cfg.model(),
                        model -> {
                            if (callCount.getAndIncrement() == 0) {
                                throw new RuntimeException("model-a failed");
                            }
                            throw secondException;
                        }));

        assertEquals(secondException, thrown);
    }

    @Test
    public void test_executeWithFallback_noModelsConfigured_throwsImmediately() {
        final ProviderConfig config = configWithModels(null);
        final Cache<String, String> cache = freshCache();

        assertThrows(
                IllegalArgumentException.class,
                () -> LangChain4jAIClient.get().executeWithFallback(
                        "test", "chat", config, cache,
                        cfg -> "ignored",
                        model -> "ignored"));
    }

    @Test
    public void test_executeWithFallback_singleModel_success() {
        final ProviderConfig config = configWithModels("gpt-4o");
        final Cache<String, String> cache = freshCache();

        final String result = LangChain4jAIClient.get().executeWithFallback(
                "test", "chat", config, cache,
                cfg -> "instance",
                model -> "ok");

        assertEquals("ok", result);
    }

    // -------------------------------------------------------------------------
    // effectiveModels — deploymentName fallback for Azure configs
    // -------------------------------------------------------------------------

    @Test
    public void test_executeWithFallback_deploymentNameOnly_usedAsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .apiKey("sk-test")
                .deploymentName("my-azure-deployment")
                .build();
        final Cache<String, String> cache = freshCache();

        final String result = LangChain4jAIClient.get().executeWithFallback(
                "test", "chat", config, cache,
                cfg -> cfg.model(),
                model -> "response-from-" + model);

        assertEquals("response-from-my-azure-deployment", result);
    }

    @Test
    public void test_executeWithFallback_modelTakesPrecedenceOverDeploymentName() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .apiKey("sk-test")
                .model("gpt-4o")
                .deploymentName("my-azure-deployment")
                .build();
        final Cache<String, String> cache = freshCache();

        final String result = LangChain4jAIClient.get().executeWithFallback(
                "test", "chat", config, cache,
                cfg -> cfg.model(),
                model -> "response-from-" + model);

        assertEquals("response-from-gpt-4o", result);
    }

    @Test
    public void test_executeWithFallback_noModelNoDeploymentName_throwsImmediately() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("azure_openai")
                .apiKey("sk-test")
                .build();
        final Cache<String, String> cache = freshCache();

        assertThrows(
                IllegalArgumentException.class,
                () -> LangChain4jAIClient.get().executeWithFallback(
                        "test", "chat", config, cache,
                        cfg -> "ignored",
                        model -> "ignored"));
    }

    // -------------------------------------------------------------------------
    // applyRequestSize — image size override from request payload
    // -------------------------------------------------------------------------

    @Test
    public void test_applyRequestSize_sizeInPayload_overridesBaseConfig() {
        final ProviderConfig base = ImmutableProviderConfig.builder()
                .provider("openai").apiKey("sk-test").model("dall-e-3")
                .size("1024x1024")
                .build();
        final JSONObject payload = new JSONObject();
        payload.put(AiKeys.SIZE, "1792x1024");

        final ProviderConfig resolved = LangChain4jAIClient.applyRequestSize(base, payload);

        assertEquals("1792x1024", resolved.size());
    }

    @Test
    public void test_applyRequestSize_noSizeInPayload_keepsBaseConfig() {
        final ProviderConfig base = ImmutableProviderConfig.builder()
                .provider("openai").apiKey("sk-test").model("dall-e-3")
                .size("1024x1024")
                .build();
        final JSONObject payload = new JSONObject();
        payload.put(AiKeys.PROMPT, "a turtle");

        final ProviderConfig resolved = LangChain4jAIClient.applyRequestSize(base, payload);

        assertEquals("1024x1024", resolved.size());
    }

    @Test
    public void test_applyRequestSize_blankSizeInPayload_keepsBaseConfig() {
        final ProviderConfig base = ImmutableProviderConfig.builder()
                .provider("openai").apiKey("sk-test").model("dall-e-3")
                .size("1024x1024")
                .build();
        final JSONObject payload = new JSONObject();
        payload.put(AiKeys.SIZE, "   ");

        final ProviderConfig resolved = LangChain4jAIClient.applyRequestSize(base, payload);

        assertEquals("1024x1024", resolved.size());
    }

    @Test
    public void test_sendRequest_disabledConfig_throws() {
        final AppConfig disabledConfig = mock(AppConfig.class);
        when(disabledConfig.isEnabled()).thenReturn(false);

        final JSONObject payload = new JSONObject();
        payload.put(AiKeys.MODEL, "gpt-4o-mini");

        final JSONObjectAIRequest request = JSONObjectAIRequest.quickText(disabledConfig, payload, "test-user");

        assertThrows(
                DotAIAppConfigDisabledException.class,
                () -> LangChain4jAIClient.get().sendRequest(request, new ByteArrayOutputStream()));
    }

}
