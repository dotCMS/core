package com.dotcms.ai.app;

import com.dotcms.security.apps.Secret;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AppConfig JSON parsing logic.
 *
 * Key scenario being tested: when a user pastes a providerConfig with long API keys
 * into the dotCMS Apps textarea, the UI may wrap those keys with literal newline
 * characters (\n). Jackson rejects JSON strings containing unescaped control chars,
 * so the parse fails silently and all models stay as NOOP_MODEL (isEnabled() = false).
 */
public class AppConfigTest {

    // 164-char dummy key split at position 80 to simulate textarea line-wrap.
    // The \n is a *literal* newline embedded inside the JSON string value.
    private static final String CHAT_API_KEY_PART1    = "sk-proj-AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String CHAT_API_KEY_PART2    = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
    private static final String EMBED_API_KEY_PART1   = "sk-proj-CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
    private static final String EMBED_API_KEY_PART2   = "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD";

    /** Clean JSON — no embedded newlines anywhere. */
    private static final String CLEAN_PROVIDER_CONFIG =
            "{\n" +
            "  \"chat\": {\n" +
            "    \"provider\": \"openai\",\n" +
            "    \"apiKey\": \"" + CHAT_API_KEY_PART1 + CHAT_API_KEY_PART2 + "\",\n" +
            "    \"model\": \"gpt-4o-mini\",\n" +
            "    \"maxTokens\": 16384\n" +
            "  },\n" +
            "  \"embeddings\": {\n" +
            "    \"provider\": \"openai\",\n" +
            "    \"apiKey\": \"" + EMBED_API_KEY_PART1 + EMBED_API_KEY_PART2 + "\",\n" +
            "    \"model\": \"text-embedding-ada-002\"\n" +
            "  }\n" +
            "}";

    /**
     * Simulates what gets stored when the textarea wraps long values:
     * the apiKey string contains a literal \n character in the middle.
     */
    private static final String WRAPPED_PROVIDER_CONFIG =
            "{\n" +
            "  \"chat\": {\n" +
            "    \"provider\": \"openai\",\n" +
            "    \"apiKey\": \"" + CHAT_API_KEY_PART1 + "\n" + CHAT_API_KEY_PART2 + "\",\n" +
            "    \"model\": \"gpt-4o-mini\",\n" +
            "    \"maxTokens\": 16384\n" +
            "  },\n" +
            "  \"embeddings\": {\n" +
            "    \"provider\": \"openai\",\n" +
            "    \"apiKey\": \"" + EMBED_API_KEY_PART1 + "\n" + EMBED_API_KEY_PART2 + "\",\n" +
            "    \"model\": \"text-embedding-ada-002\"\n" +
            "  }\n" +
            "}";

    // -------------------------------------------------------------------------
    // Real-world JSON format from the dotAI Apps config (formatted, 3 sections)
    // API key matches the actual structure: sk-proj-<164 chars>
    // -------------------------------------------------------------------------

    private static final String REAL_API_KEY =
            "sk-proj-EEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" +
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";

    /** Exact format the user pastes into the Apps UI textarea (pretty-printed, 3 sections). */
    private static final String REAL_WORLD_FORMATTED_CONFIG =
            "{\n" +
            "   \"chat\":{\n" +
            "      \"provider\":\"openai\",\n" +
            "      \"apiKey\":\"" + REAL_API_KEY + "\",\n" +
            "      \"model\":\"o4-mini\",\n" +
            "      \"maxCompletionTokens\":16384,\n" +
            "      \"temperature\":1.0,\n" +
            "      \"maxRetries\":3\n" +
            "   },\n" +
            "   \"embeddings\":{\n" +
            "      \"provider\":\"openai\",\n" +
            "      \"apiKey\":\"" + REAL_API_KEY + "\",\n" +
            "      \"model\":\"text-embedding-3-small\"\n" +
            "   },\n" +
            "   \"image\":{\n" +
            "      \"provider\":\"openai\",\n" +
            "      \"apiKey\":\"" + REAL_API_KEY + "\",\n" +
            "      \"model\":\"gpt-image-1\",\n" +
            "      \"size\":\"1024x1024\"\n" +
            "   }\n" +
            "}";

    @Test
    public void test_parseProviderConfig_realWorldFormattedJson_parsesAllSections() {
        final JsonNode root = AppConfig.parseProviderConfig(REAL_WORLD_FORMATTED_CONFIG);

        assertFalse("Formatted JSON should parse successfully", root.isEmpty());
        assertEquals("o4-mini",                root.path("chat").path("model").asText());
        assertEquals("text-embedding-3-small", root.path("embeddings").path("model").asText());
        assertEquals("gpt-image-1",            root.path("image").path("model").asText());
    }

    @Test
    public void test_appConfig_realWorldFormattedJson_isEnabled_allModelsSet() {
        final AppConfig config = buildAppConfig(REAL_WORLD_FORMATTED_CONFIG);

        assertTrue("AppConfig should be enabled with real-world formatted providerConfig", config.isEnabled());
        assertEquals("o4-mini",                config.getModel().getCurrentModel());
        assertEquals("text-embedding-3-small", config.getEmbeddingsModel().getCurrentModel());
        assertEquals("gpt-image-1",            config.getImageModel().getCurrentModel());
    }

    // -------------------------------------------------------------------------
    // parseProviderConfig — unit tests on the static method directly
    // -------------------------------------------------------------------------

    @Test
    public void test_parseProviderConfig_cleanJson_parsesModelNames() {
        final JsonNode root = AppConfig.parseProviderConfig(CLEAN_PROVIDER_CONFIG);

        assertFalse("Parse should not return empty node for valid JSON", root.isEmpty());
        assertEquals("gpt-4o-mini",           root.path("chat").path("model").asText());
        assertEquals("text-embedding-ada-002", root.path("embeddings").path("model").asText());
    }

    // Note: parseProviderConfig receives already-sanitized JSON (sanitization happens in the
    // constructor before calling this method). Embedded-newline scenarios are covered by
    // test_appConfig_withWrappedProviderConfig_isEnabled below.

    @Test
    public void test_parseProviderConfig_null_returnsEmptyObjectNode() {
        final JsonNode root = AppConfig.parseProviderConfig(null);

        assertNotNull(root);
        assertTrue("Null input should yield empty ObjectNode, not a missing node", root.isObject());
        assertEquals(0, root.size());
    }

    @Test
    public void test_parseProviderConfig_emptyString_doesNotThrow() {
        // Empty string reaches Jackson's readTree which returns NullNode (not an ObjectNode).
        // This path never happens in production since the constructor guards with isNotBlank,
        // but we verify at least that it doesn't throw.
        final JsonNode root = AppConfig.parseProviderConfig("");
        assertNotNull(root);
    }

    @Test
    public void test_parseProviderConfig_invalidJson_returnsEmptyObjectNode() {
        final JsonNode root = AppConfig.parseProviderConfig("this is not json at all");

        assertNotNull(root);
        assertTrue(root.isObject());
        assertEquals(0, root.size());
    }

    // -------------------------------------------------------------------------
    // AppConfig constructor — end-to-end: wrapped config → isEnabled() + models
    // -------------------------------------------------------------------------

    @Test
    public void test_appConfig_withCleanProviderConfig_isEnabled() {
        final AppConfig config = buildAppConfig(CLEAN_PROVIDER_CONFIG);

        assertTrue("AppConfig should be enabled when providerConfig is valid", config.isEnabled());
        assertEquals("gpt-4o-mini", config.getModel().getCurrentModel());
        assertEquals("text-embedding-ada-002", config.getEmbeddingsModel().getCurrentModel());
    }

    @Test
    public void test_appConfig_withWrappedProviderConfig_isEnabled() {
        // This is the scenario that should work but was failing before the fix
        final AppConfig config = buildAppConfig(WRAPPED_PROVIDER_CONFIG);

        assertTrue("AppConfig should be enabled even when apiKey contains embedded newlines", config.isEnabled());
        assertEquals("gpt-4o-mini", config.getModel().getCurrentModel());
        assertEquals("text-embedding-ada-002", config.getEmbeddingsModel().getCurrentModel());
    }

    @Test
    public void test_appConfig_withBlankProviderConfig_isNotEnabled() {
        final AppConfig config = buildAppConfig("   ");

        assertFalse(config.isEnabled());
    }

    @Test
    public void test_appConfig_withNullProviderConfig_isNotEnabled() {
        final AppConfig config = buildAppConfig(null);

        assertFalse(config.isEnabled());
    }

    // -------------------------------------------------------------------------
    // getConfig — settings section overrides defaults
    // -------------------------------------------------------------------------

    private static final String CONFIG_WITH_SETTINGS =
            "{\n" +
            "  \"chat\": {\"provider\": \"openai\", \"apiKey\": \"sk-test\", \"model\": \"gpt-4o-mini\"},\n" +
            "  \"settings\": {\n" +
            "    \"rolePrompt\": \"Custom role prompt\",\n" +
            "    \"textPrompt\": \"Custom text prompt\",\n" +
            "    \"imagePrompt\": \"Custom image prompt\",\n" +
            "    \"imageSize\": \"1792x1024\",\n" +
            "    \"temperature\": \"0.5\"\n" +
            "  }\n" +
            "}";

    @Test
    public void test_getConfig_settingsValue_overridesDefault() {
        final AppConfig config = buildAppConfig(CONFIG_WITH_SETTINGS);

        assertEquals("Custom role prompt", config.getConfig(AppKeys.ROLE_PROMPT));
        assertEquals("Custom text prompt", config.getConfig(AppKeys.TEXT_PROMPT));
        assertEquals("Custom image prompt", config.getConfig(AppKeys.IMAGE_PROMPT));
        assertEquals("1792x1024", config.getConfig(AppKeys.IMAGE_SIZE));
        assertEquals("0.5", config.getConfig(AppKeys.COMPLETION_TEMPERATURE));
    }

    @Test
    public void test_getConfig_settingsValueAbsent_returnsFallback() {
        // Config has settings section but NOT these keys → should return AppKeys defaults
        final AppConfig config = buildAppConfig(CONFIG_WITH_SETTINGS);

        assertEquals(AppKeys.LISTENER_INDEXER.defaultValue, config.getConfig(AppKeys.LISTENER_INDEXER));
        assertEquals(AppKeys.EMBEDDINGS_SPLIT_AT_TOKENS.defaultValue,
                config.getConfig(AppKeys.EMBEDDINGS_SPLIT_AT_TOKENS));
    }

    @Test
    public void test_getConfig_noSettingsSection_returnsDefault() {
        // Clean config with no settings block at all
        final AppConfig config = buildAppConfig(CLEAN_PROVIDER_CONFIG);

        assertEquals(AppKeys.ROLE_PROMPT.defaultValue, config.getConfig(AppKeys.ROLE_PROMPT));
        assertEquals(AppKeys.IMAGE_SIZE.defaultValue, config.getConfig(AppKeys.IMAGE_SIZE));
        assertEquals(AppKeys.COMPLETION_TEMPERATURE.defaultValue, config.getConfig(AppKeys.COMPLETION_TEMPERATURE));
    }

    @Test
    public void test_getConfig_keyWithNoSettingsKey_alwaysReturnsDefault() {
        // PROVIDER_CONFIG and API_URL have settingsKey=null — settings section never consulted
        final AppConfig config = buildAppConfig(CONFIG_WITH_SETTINGS);

        assertNull(config.getConfig(AppKeys.PROVIDER_CONFIG));
        assertEquals(AppKeys.API_URL.defaultValue, config.getConfig(AppKeys.API_URL));
    }

    @Test
    public void test_appConfig_settingsSection_populatesPromptFields() {
        final AppConfig config = buildAppConfig(CONFIG_WITH_SETTINGS);

        assertEquals("Custom role prompt", config.getRolePrompt());
        assertEquals("Custom text prompt", config.getTextPrompt());
        assertEquals("Custom image prompt", config.getImagePrompt());
        assertEquals("1792x1024", config.getImageSize());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AppConfig buildAppConfig(final String providerConfigJson) {
        final Map<String, Secret> secrets = new HashMap<>();
        if (providerConfigJson != null) {
            final Secret providerConfigSecret = mock(Secret.class);
            when(providerConfigSecret.getString()).thenReturn(providerConfigJson);
            secrets.put(AppKeys.PROVIDER_CONFIG.key, providerConfigSecret);
        }
        return new AppConfig("localhost", secrets);
    }

}
