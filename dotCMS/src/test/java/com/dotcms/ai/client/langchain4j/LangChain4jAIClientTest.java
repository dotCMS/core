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
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
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

    /**
     * Given a null messages array,
     * When toMessages is called,
     * Then an empty list is returned.
     */
    @Test
    public void test_toMessages_null_returnsEmptyList() {
        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(null);
        assertTrue(messages.isEmpty());
    }

    /**
     * Given a messages array with a system-role entry,
     * When toMessages is called,
     * Then a SystemMessage with the correct text is returned.
     */
    @Test
    public void test_toMessages_systemRole_producesSystemMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "system", AiKeys.CONTENT, "You are helpful."));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof SystemMessage);
        assertEquals("You are helpful.", ((SystemMessage) messages.get(0)).text());
    }

    /**
     * Given a messages array with an assistant-role entry,
     * When toMessages is called,
     * Then an AiMessage with the correct text is returned.
     */
    @Test
    public void test_toMessages_assistantRole_producesAiMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "assistant", AiKeys.CONTENT, "I can help."));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof AiMessage);
        assertEquals("I can help.", ((AiMessage) messages.get(0)).text());
    }

    /**
     * Given a messages array with a user-role entry,
     * When toMessages is called,
     * Then a UserMessage with the correct text is returned.
     */
    @Test
    public void test_toMessages_userRole_producesUserMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "user", AiKeys.CONTENT, "Hello!"));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
        assertEquals("Hello!", ((UserMessage) messages.get(0)).singleText());
    }

    /**
     * Given a messages array with an unrecognized role,
     * When toMessages is called,
     * Then the entry defaults to a UserMessage.
     */
    @Test
    public void test_toMessages_unknownRole_defaultsToUserMessage() {
        final JSONArray array = new JSONArray();
        array.put(Map.of(AiKeys.ROLE, "unknown-role", AiKeys.CONTENT, "Some text"));

        final List<ChatMessage> messages = LangChain4jAIClient.toMessages(array);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
    }

    /**
     * Given a messages array with system, user, and assistant entries in order,
     * When toMessages is called,
     * Then three messages are returned in the original order with correct types.
     */
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

    /**
     * Given a user message whose content field is a JSON array with a text part and an image_url part,
     * When toMessages is called,
     * Then a UserMessage with both TextContent and ImageContent is returned.
     */
    @Test
    public void test_toMessages_multimodalContentArray_producesUserMessageWithImageAndText() {
        final JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        final JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/webp;base64,ABC123");
        imagePart.put("image_url", imageUrl);

        final JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", "Describe this image");

        final JSONArray contentArray = new JSONArray();
        contentArray.put(textPart);
        contentArray.put(imagePart);

        final JSONObject msg = new JSONObject();
        msg.put(AiKeys.ROLE, "user");
        msg.put(AiKeys.CONTENT, contentArray);

        final JSONArray messages = new JSONArray();
        messages.put(msg);

        final List<ChatMessage> result = LangChain4jAIClient.toMessages(messages);

        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof UserMessage);
        final UserMessage userMessage = (UserMessage) result.get(0);
        assertEquals(2, userMessage.contents().size());
        assertTrue(userMessage.contents().get(0) instanceof TextContent);
        assertTrue(userMessage.contents().get(1) instanceof ImageContent);
    }

    /**
     * Given a user message whose content field is a JSON array containing an image_url entry,
     * When toMessages is called,
     * Then the content array is parsed as structured ImageContent rather than serialized to a plain string.
     */
    @Test
    public void test_toMessages_contentArrayNotTreatedAsString() {
        // Regression: before the fix, JSONArray.optString() would serialize the array
        // to its toString() representation and pass it as a plain text message,
        // so the model would never see the actual image data.
        final JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        final JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/png;base64,iVBORw0KGgo=");
        imagePart.put("image_url", imageUrl);

        final JSONArray contentArray = new JSONArray();
        contentArray.put(imagePart);

        final JSONObject msg = new JSONObject();
        msg.put(AiKeys.ROLE, "user");
        msg.put(AiKeys.CONTENT, contentArray);

        final JSONArray messages = new JSONArray();
        messages.put(msg);

        final List<ChatMessage> result = LangChain4jAIClient.toMessages(messages);

        final UserMessage userMessage = (UserMessage) result.get(0);
        // Must have structured content, not a plain text message
        final List<Content> contents = userMessage.contents();
        assertEquals(1, contents.size());
        assertTrue(contents.get(0) instanceof ImageContent);
    }

    /**
     * Given a content array with an image_url entry whose URL is a data URI (data:image/webp;base64,...),
     * When toMultimodalUserMessage is called,
     * Then an ImageContent is produced with the correct MIME type and base64 data extracted.
     */
    @Test
    public void test_toMultimodalUserMessage_dataUri_extractsMimeTypeAndBase64() {
        final JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        final JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/webp;base64,ABC123==");
        imagePart.put("image_url", imageUrl);

        final JSONArray contentArray = new JSONArray();
        contentArray.put(imagePart);

        final UserMessage msg = LangChain4jAIClient.toMultimodalUserMessage(contentArray);

        assertEquals(1, msg.contents().size());
        final ImageContent imageContent = (ImageContent) msg.contents().get(0);
        assertEquals("ABC123==", imageContent.image().base64Data());
        assertEquals("image/webp", imageContent.image().mimeType());
    }

    /**
     * Given a content array with an image_url entry whose URL is a plain HTTPS URL,
     * When toMultimodalUserMessage is called,
     * Then an ImageContent is produced with the URL preserved.
     */
    @Test
    public void test_toMultimodalUserMessage_plainUrl_producesImageContentWithUrl() {
        final JSONObject imagePart = new JSONObject();
        imagePart.put("type", "image_url");
        final JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "https://example.com/photo.jpg");
        imagePart.put("image_url", imageUrl);

        final JSONArray contentArray = new JSONArray();
        contentArray.put(imagePart);

        final UserMessage msg = LangChain4jAIClient.toMultimodalUserMessage(contentArray);

        assertEquals(1, msg.contents().size());
        final ImageContent imageContent = (ImageContent) msg.contents().get(0);
        assertEquals("https://example.com/photo.jpg", imageContent.image().url().toString());
    }

    /**
     * Given a content array with a text-type entry,
     * When toMultimodalUserMessage is called,
     * Then a TextContent with the correct text is produced.
     */
    @Test
    public void test_toMultimodalUserMessage_textType_producesTextContent() {
        final JSONObject textPart = new JSONObject();
        textPart.put("type", "text");
        textPart.put("text", "What is in this image?");

        final JSONArray contentArray = new JSONArray();
        contentArray.put(textPart);

        final UserMessage msg = LangChain4jAIClient.toMultimodalUserMessage(contentArray);

        assertEquals(1, msg.contents().size());
        assertTrue(msg.contents().get(0) instanceof TextContent);
        assertEquals("What is in this image?", ((TextContent) msg.contents().get(0)).text());
    }

    /**
     * Given a ChatResponse with an assistant message and a model name,
     * When toChatResponseJson is called,
     * Then the resulting JSON has the correct OpenAI-compatible structure with role, content, and model.
     */
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

    /**
     * Given an Embedding with three float values,
     * When toEmbeddingResponseJson is called,
     * Then the resulting JSON contains a data array with the values stored as doubles with correct precision.
     */
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

    /**
     * Given an Image with a URL,
     * When toImageResponseJson is called,
     * Then the resulting JSON contains the URL in the data array.
     */
    @Test
    public void test_toImageResponseJson_containsUrl() throws Exception {
        final Image image = Image.builder().url(new URI("https://example.com/image.png")).build();

        final JSONObject json = new JSONObject(LangChain4jAIClient.toImageResponseJson(image));
        final String url = json.getJSONArray(AiKeys.DATA).getJSONObject(0).getString(AiKeys.URL);

        assertEquals("https://example.com/image.png", url);
    }

    /**
     * Given an Image with no URL set,
     * When toImageResponseJson is called,
     * Then the URL field in the data array is an empty string.
     */
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

    /**
     * Given a config with two models where the builder throws for the first,
     * When executeWithFallback is called,
     * Then the second model is used and its response is returned.
     */
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

    /**
     * Given a config with two models where the executor throws on the first invocation,
     * When executeWithFallback is called,
     * Then execution falls back to the second model and returns its response.
     */
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

    /**
     * Given a config with two models where the executor throws for both,
     * When executeWithFallback is called,
     * Then the exception from the last model is rethrown.
     */
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

    /**
     * Given a config with no model name set,
     * When executeWithFallback is called,
     * Then an IllegalArgumentException is thrown immediately without invoking the builder or executor.
     */
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

    /**
     * Given a config with a single model that succeeds,
     * When executeWithFallback is called,
     * Then the response is returned without fallback.
     */
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

    /**
     * Given an Azure config with only a deploymentName and no model,
     * When executeWithFallback is called,
     * Then the deploymentName is used as the model identifier.
     */
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

    /**
     * Given an Azure config with both model and deploymentName set,
     * When executeWithFallback is called,
     * Then the model field takes precedence over deploymentName.
     */
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

    /**
     * Given an Azure config with neither model nor deploymentName set,
     * When executeWithFallback is called,
     * Then an IllegalArgumentException is thrown immediately.
     */
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

    /**
     * Given a base config with a default size and a request payload that specifies a different size,
     * When applyRequestSize is called,
     * Then the resolved config uses the size from the payload.
     */
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

    /**
     * Given a base config with a default size and a request payload that has no size field,
     * When applyRequestSize is called,
     * Then the resolved config keeps the base config size.
     */
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

    /**
     * Given a base config with a default size and a request payload with a blank size value,
     * When applyRequestSize is called,
     * Then the resolved config keeps the base config size.
     */
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

    /**
     * Given an AppConfig where isEnabled() returns false,
     * When sendRequest is called,
     * Then a DotAIAppConfigDisabledException is thrown.
     */
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
