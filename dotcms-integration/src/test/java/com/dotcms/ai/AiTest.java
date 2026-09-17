package com.dotcms.ai;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.WireMockTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.owasp.encoder.Encode;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    /** Active markup returned by the {@code ai-escape-*} WireMock stubs (#37153). */
    String PROBE_MARKUP = "<script>alert(1)</script><img src=x onerror=alert(1)>";
    /** {@link #PROBE_MARKUP} after OWASP {@code Encode.forHtml}. */
    String PROBE_MARKUP_ESCAPED = "&lt;script&gt;alert(1)&lt;/script&gt;&lt;img src=x onerror=alert(1)&gt;";
    /** Any chat prompt containing this text is answered by {@code ai-escape-chat-stub.json}. */
    String PROBE_CHAT_PROMPT = "Escaping probe markup";
    /** Any chat prompt containing this text is answered with HTTP 500 by {@code ai-escape-chat-error-stub.json}. */
    String PROBE_FAILURE_PROMPT = "Escaping probe failure";
    /**
     * Exact image prompt answered by {@code ai-escape-image-stub.json}. Carries markup because the image
     * payload's only text is the echoed {@code originalPrompt} (the langchain4j client rebuilds the provider
     * response as {@code {data:[{url}]}}; no {@code revised_prompt} reaches dotCMS).
     */
    String PROBE_IMAGE_PROMPT = "Escaping probe image <img src=x onerror=alert(1)>";

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

    // ------------------------------------------------------------------------------------------
    // Payload assertions shared by the viewtool escaping tests (#37153)
    // ------------------------------------------------------------------------------------------

    /** Fails if any string leaf at any depth contains a raw {@code < > " '}. */
    static void assertNoRawMarkup(final Object value) {
        walk("", value, (path, leaf) -> {
            if (leaf instanceof String) {
                final String s = (String) leaf;
                assertFalse("raw markup at " + path + ": " + s,
                        s.contains("<") || s.contains(">") || s.contains("\"") || s.contains("'"));
            }
        });
    }

    /**
     * Deep equality including string values, skipping the given key names wherever they appear
     * (for values that legitimately differ between two calls, e.g. {@code timeToEmbeddings}).
     */
    static void assertEquivalentIgnoring(final Object expected, final Object actual, final Set<String> ignoredKeys) {
        assertSameShape("", expected, actual, ignoredKeys);
        walkPair("", expected, actual, ignoredKeys, (path, e, a) -> {
            if (e instanceof String) {
                assertEquals("string at " + path, e, a);
            }
        });
    }

    /**
     * Compares an unescaped payload with its escaped twin: every string leaf whose path satisfies
     * {@code escapedPath} must equal {@code Encode.forHtml(unescaped)}; every other string leaf must
     * be identical. Paths look like {@code dotCMSResults[0].matches[0].extractedText}.
     */
    static void assertEscapedOnlyAt(final Object unsafe, final Object escaped, final Predicate<String> escapedPath) {
        assertEscapedOnlyAt(unsafe, escaped, escapedPath, Set.of());
    }

    /** As {@link #assertEscapedOnlyAt(Object, Object, Predicate)}, skipping keys that differ per call. */
    static void assertEscapedOnlyAt(final Object unsafe, final Object escaped, final Predicate<String> escapedPath,
                                    final Set<String> ignoredKeys) {
        assertSameShape("", unsafe, escaped, ignoredKeys);
        walkPair("", unsafe, escaped, ignoredKeys, (path, u, e) -> {
            if (u instanceof String) {
                if (escapedPath.test(path)) {
                    assertEquals("expected escaped string at " + path, Encode.forHtml((String) u), e);
                } else {
                    assertEquals("expected untouched string at " + path, u, e);
                }
            }
        });
    }

    /** Path predicate for the summarize / search shape: provider subtree, query, error, extractedText. */
    static Predicate<String> searchShapedEscapedPaths() {
        return path -> path.startsWith("openAiResponse")
                || path.equals("query") || path.equals("error")
                || path.endsWith(".extractedText");
    }

    /** Finds the {@code dotCMSResults} element whose {@code title} equals {@code title}, or fails. */
    static JSONObject findResultByTitle(final JSONObject payload, final String title) {
        final JSONArray results = payload.getJSONArray("dotCMSResults");
        for (int i = 0; i < results.length(); i++) {
            final Object element = results.get(i);
            if (element instanceof JSONObject && title.equals(((JSONObject) element).opt("title"))) {
                return (JSONObject) element;
            }
        }
        throw new AssertionError("no dotCMSResults element with title " + title + " in " + payload);
    }

    /** {@code openAiResponse.choices[0].message.content} of a summarize payload. */
    static String summarizeContent(final JSONObject payload) {
        return payload.getJSONObject("openAiResponse").getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content");
    }

    /** {@code choices[0].message.content} of a raw / generateText payload. */
    static String chatContent(final JSONObject payload) {
        return payload.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    interface LeafVisitor { void visit(String path, Object leaf); }
    interface PairVisitor { void visit(String path, Object expected, Object actual); }

    static void walk(final String path, final Object value, final LeafVisitor visitor) {
        if (value instanceof Map) {
            for (final Object key : ((Map<?, ?>) value).keySet()) {
                walk(path.isEmpty() ? String.valueOf(key) : path + "." + key, ((Map<?, ?>) value).get(key), visitor);
            }
        } else if (value instanceof java.util.List) {
            final java.util.List<?> list = (java.util.List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                walk(path + "[" + i + "]", list.get(i), visitor);
            }
        } else {
            visitor.visit(path, value);
        }
    }

    static void assertSameShape(final String path, final Object expected, final Object actual, final Set<String> ignoredKeys) {
        if (expected instanceof Map) {
            assertTrue("expected object at " + path + " but was " + actual, actual instanceof Map);
            final Map<?, ?> e = (Map<?, ?>) expected;
            final Map<?, ?> a = (Map<?, ?>) actual;
            assertEquals("keys at " + path, e.keySet(), a.keySet());
            for (final Object key : e.keySet()) {
                if (!ignoredKeys.contains(String.valueOf(key))) {
                    assertSameShape(path.isEmpty() ? String.valueOf(key) : path + "." + key, e.get(key), a.get(key), ignoredKeys);
                }
            }
        } else if (expected instanceof java.util.List) {
            assertTrue("expected array at " + path + " but was " + actual, actual instanceof java.util.List);
            final java.util.List<?> e = (java.util.List<?>) expected;
            final java.util.List<?> a = (java.util.List<?>) actual;
            assertEquals("length at " + path, e.size(), a.size());
            for (int i = 0; i < e.size(); i++) {
                assertSameShape(path + "[" + i + "]", e.get(i), a.get(i), ignoredKeys);
            }
        } else if (!(expected instanceof String)) {
            assertEquals("leaf at " + path, expected, actual);
        }
    }

    static void walkPair(final String path, final Object expected, final Object actual, final Set<String> ignoredKeys, final PairVisitor visitor) {
        if (expected instanceof Map) {
            final Map<?, ?> e = (Map<?, ?>) expected;
            final Map<?, ?> a = (Map<?, ?>) actual;
            for (final Object key : e.keySet()) {
                if (!ignoredKeys.contains(String.valueOf(key))) {
                    walkPair(path.isEmpty() ? String.valueOf(key) : path + "." + key, e.get(key), a.get(key), ignoredKeys, visitor);
                }
            }
        } else if (expected instanceof java.util.List) {
            final java.util.List<?> e = (java.util.List<?>) expected;
            final java.util.List<?> a = (java.util.List<?>) actual;
            for (int i = 0; i < e.size(); i++) {
                walkPair(path + "[" + i + "]", e.get(i), a.get(i), ignoredKeys, visitor);
            }
        } else {
            visitor.visit(path, expected, actual);
        }
    }

}
