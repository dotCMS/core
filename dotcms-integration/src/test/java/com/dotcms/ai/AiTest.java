package com.dotcms.ai;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.viewtool.AIViewToolErrorHandler;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.WireMockTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public interface AiTest {

    String API_URL = "http://localhost:%d/c";
    String API_IMAGE_URL = "http://localhost:%d/i";
    String API_EMBEDDINGS_URL = "http://localhost:%d/e";
    String API_KEY = "some-api-key-1a2bc3";
    String MODEL = "gpt-3.5-turbo-16k";
    String IMAGE_MODEL = "dall-e-3";
    String EMBEDDINGS_MODEL = "text-embedding-ada-002";
    String IMAGE_SIZE = "1024x1024";
    int PORT = 50505;

    /**
     * Sentinels matched by the {@code ai-error-*-stub.json} WireMock mappings. A request whose
     * body contains one of these receives an HTTP 500 from the mocked provider. One sentinel per
     * provider path, so a forced chat failure never trips the embeddings stub and vice versa.
     */
    String FORCE_CHAT_ERROR = "DOTAI_FORCE_CHAT_ERROR";
    String FORCE_EMBEDDINGS_ERROR = "DOTAI_FORCE_EMBEDDINGS_ERROR";
    String FORCE_IMAGE_ERROR = "DOTAI_FORCE_IMAGE_ERROR";

    /**
     * Asserts that a viewtool failure payload is exactly what {@link AIViewToolErrorHandler} builds
     * (#37154): a {@link JSONObject} with exactly one key, {@code error}, holding the fixed generic
     * message, no {@code stackTrace} key, and no string value anywhere in it that looks like
     * exception or stack-frame text.
     *
     * <p>The equality check against the handler's constant matters: a provider error body such as
     * {@code {"error":{"message":...,"type":...}}} returned unparsed through a success path is
     * also a single-key {@code error} object with no trace markers, and would otherwise pass.</p>
     *
     * @param result the object a viewtool method returned on failure
     */
    static void assertSafeErrorPayload(final Object result) {
        assertNotNull("viewtool returned null on failure", result);
        assertTrue("failure payload must be a JSONObject, was " + result.getClass().getName(),
                result instanceof JSONObject);
        final JSONObject payload = (JSONObject) result;
        assertTrue("failure payload must carry an 'error' key", payload.containsKey("error"));
        assertFalse("failure payload must not carry a 'stackTrace' key", payload.containsKey("stackTrace"));
        assertEquals("failure payload must carry exactly one key, got " + payload.keySet(), 1, payload.size());
        assertEquals("failure payload must be the handler's fixed message",
                AIViewToolErrorHandler.GENERIC_ERROR_MESSAGE, payload.get("error"));
        assertNoInternalDetail(payload);
    }

    /**
     * Recursively checks every string value in a JSON structure for markers of leaked internals.
     */
    static void assertNoInternalDetail(final Object value) {
        if (value instanceof JSONObject) {
            final JSONObject json = (JSONObject) value;
            for (final Object key : json.keySet()) {
                assertNoInternalDetail(json.get(key));
            }
        } else if (value instanceof JSONArray) {
            final JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                assertNoInternalDetail(array.get(i));
            }
        } else if (value instanceof String) {
            final String text = (String) value;
            for (final String marker : new String[] {"Exception", "\tat ", ".java:", "com.dotcms"}) {
                assertFalse("failure payload leaks internal detail (" + marker.trim() + "): " + text,
                        text.contains(marker));
            }
        }
    }

    static WireMockServer prepareWireMock() {
        final WireMockServer wireMockServer = WireMockTestHelper.wireMockServer(PORT);
        wireMockServer.start();

        return wireMockServer;
    }

    static void removeAiAppSecrets(final Host host) throws Exception {
        APILocator.getAppsAPI().deleteSecrets(AppKeys.APP_KEY, host, APILocator.systemUser());
    }

    static String providerConfigJson(final int port, final String chatModel) {
        final String endpoint = String.format("http://localhost:%d/", port);
        return String.format(
                "{" +
                "\"chat\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\",\"endpoint\":\"%s\",\"maxRetries\":0}," +
                "\"embeddings\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\",\"endpoint\":\"%s\",\"maxRetries\":0}," +
                "\"image\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\",\"endpoint\":\"%s\",\"maxRetries\":0}," +
                "\"settings\":{\"listenerIndexer\":{\"default\":\"blog\"}}" +
                "}",
                API_KEY, chatModel, endpoint,
                API_KEY, EMBEDDINGS_MODEL, endpoint,
                API_KEY, IMAGE_MODEL, endpoint);
    }

    static Map<String, Secret> aiAppSecretsWithProviderConfig(
            final Host host, final String providerConfigJson) throws Exception {
        final AppSecrets appSecrets = new AppSecrets.Builder()
                .withKey(AppKeys.APP_KEY)
                .withSecret(AppKeys.PROVIDER_CONFIG.key, providerConfigJson)
                .build();
        APILocator.getAppsAPI().saveSecrets(appSecrets, host, APILocator.systemUser());
        await().atMost(5, SECONDS).until(() -> ConfigService.INSTANCE.config(host).isEnabled());
        return appSecrets.getSecrets();
    }

}
