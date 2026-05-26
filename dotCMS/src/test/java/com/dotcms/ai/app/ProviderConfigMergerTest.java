package com.dotcms.ai.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProviderConfigMergerTest {

    // -------------------------------------------------------------------------
    // containsMasked
    // -------------------------------------------------------------------------

    @Test
    public void test_containsMasked_withMaskedValue_returnsTrue() {
        assertTrue(ProviderConfigMerger.containsMasked("{\"apiKey\":\"*****\"}"));
    }

    @Test
    public void test_containsMasked_withoutMaskedValue_returnsFalse() {
        assertFalse(ProviderConfigMerger.containsMasked("{\"apiKey\":\"sk-real-key\"}"));
    }

    @Test
    public void test_containsMasked_blankInput_returnsFalse() {
        assertFalse(ProviderConfigMerger.containsMasked(""));
        assertFalse(ProviderConfigMerger.containsMasked(null));
    }

    @Test
    public void test_containsMasked_partialMatch_doesNotMatch() {
        // "****" (4 stars) is not the sentinel — must be exactly "*****" (5)
        assertFalse(ProviderConfigMerger.containsMasked("{\"apiKey\":\"****\"}"));
    }

    // -------------------------------------------------------------------------
    // containsMaskedCredential
    // -------------------------------------------------------------------------

    @Test
    public void test_containsMaskedCredential_maskedApiKey_returnsTrue() {
        assertTrue(ProviderConfigMerger.containsMaskedCredential("{\"apiKey\":\"*****\"}"));
    }

    @Test
    public void test_containsMaskedCredential_maskedNestedApiKey_returnsTrue() {
        assertTrue(ProviderConfigMerger.containsMaskedCredential("{\"chat\":{\"apiKey\":\"*****\"}}"));
    }

    @Test
    public void test_containsMaskedCredential_maskedNonCredentialField_returnsFalse() {
        // rolePrompt with ***** is not a credential field — must not block the save
        assertFalse(ProviderConfigMerger.containsMaskedCredential("{\"chat\":{\"rolePrompt\":\"*****\",\"apiKey\":\"sk-real\"}}"));
    }

    @Test
    public void test_containsMaskedCredential_realCredentials_returnsFalse() {
        assertFalse(ProviderConfigMerger.containsMaskedCredential("{\"apiKey\":\"sk-real\",\"model\":\"gpt-4o\"}"));
    }

    @Test
    public void test_containsMaskedCredential_blankInput_returnsFalse() {
        assertFalse(ProviderConfigMerger.containsMaskedCredential(""));
        assertFalse(ProviderConfigMerger.containsMaskedCredential(null));
    }

    // -------------------------------------------------------------------------
    // merge — stored is blank
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_blankStored_returnsNewJsonUnchanged() {
        final String newJson = "{\"chat\":{\"apiKey\":\"sk-new\"}}";
        assertEquals(newJson, ProviderConfigMerger.merge(newJson, ""));
        assertEquals(newJson, ProviderConfigMerger.merge(newJson, null));
    }

    // -------------------------------------------------------------------------
    // merge — no masked values
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_noMaskedValues_returnsNewJsonStructure() throws Exception {
        final String newJson    = "{\"chat\":{\"apiKey\":\"sk-new\",\"model\":\"gpt-4o\"}}";
        final String storedJson = "{\"chat\":{\"apiKey\":\"sk-stored\",\"model\":\"gpt-3.5\"}}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        // new values win when no sentinel is present
        assertTrue(result.contains("sk-new"));
        assertTrue(result.contains("gpt-4o"));
        assertFalse(result.contains("sk-stored"));
    }

    // -------------------------------------------------------------------------
    // merge — masked top-level credential field
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_maskedTopLevelCredentialField_restoredFromStored() {
        final String newJson    = "{\"apiKey\":\"*****\"}";
        final String storedJson = "{\"apiKey\":\"sk-real-key\"}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertTrue(result.contains("sk-real-key"));
        assertFalse(result.contains("*****"));
    }

    @Test
    public void test_merge_maskedNonCredentialField_leftAsIs() {
        // Only CREDENTIAL_FIELDS are restored — other fields with ***** stay as-is
        final String newJson    = "{\"someKey\":\"*****\"}";
        final String storedJson = "{\"someKey\":\"real-value\"}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertTrue(result.contains("*****"));
        assertFalse(result.contains("real-value"));
    }

    // -------------------------------------------------------------------------
    // merge — masked nested credential field
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_maskedNestedApiKey_restoredFromStored() {
        final String newJson    = "{\"chat\":{\"apiKey\":\"*****\",\"model\":\"gpt-4o\"}}";
        final String storedJson = "{\"chat\":{\"apiKey\":\"sk-real-key\",\"model\":\"gpt-3.5\"}}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertTrue(result.contains("sk-real-key"));
        assertTrue(result.contains("gpt-4o"));   // non-credential field from new wins
        assertFalse(result.contains("*****"));
    }

    @Test
    public void test_merge_allThreeSections_maskedCredsRestored() {
        final String newJson = "{"
                + "\"chat\":{\"apiKey\":\"*****\",\"model\":\"gpt-4o\"},"
                + "\"embeddings\":{\"apiKey\":\"*****\",\"model\":\"ada-002\"},"
                + "\"image\":{\"apiKey\":\"*****\",\"model\":\"dall-e-3\"}"
                + "}";
        final String storedJson = "{"
                + "\"chat\":{\"apiKey\":\"sk-chat\",\"model\":\"old-model\"},"
                + "\"embeddings\":{\"apiKey\":\"sk-embed\",\"model\":\"old-embed\"},"
                + "\"image\":{\"apiKey\":\"sk-image\",\"model\":\"old-image\"}"
                + "}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertTrue(result.contains("sk-chat"));
        assertTrue(result.contains("sk-embed"));
        assertTrue(result.contains("sk-image"));
        assertTrue(result.contains("gpt-4o"));
        assertTrue(result.contains("ada-002"));
        assertTrue(result.contains("dall-e-3"));
        assertFalse(result.contains("*****"));
    }

    // -------------------------------------------------------------------------
    // merge — masked field not present in stored
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_maskedFieldAbsentInStored_leftAsIs() {
        final String newJson    = "{\"chat\":{\"secretAccessKey\":\"*****\"}}";
        final String storedJson = "{\"chat\":{\"model\":\"gpt-4o\"}}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        // no stored value to restore — sentinel stays (better than silently losing it)
        assertTrue(result.contains("*****"));
    }

    @Test
    public void test_merge_maskedCredentialsJson_restoredFromStored() {
        final String realJson = "{\"type\":\"service_account\",\"private_key\":\"-----BEGIN PRIVATE KEY-----\\nABC\\n-----END PRIVATE KEY-----\\n\"}";
        final String newJson    = "{\"chat\":{\"provider\":\"vertex_ai\",\"credentialsJson\":\"*****\"}}";
        final String storedJson = "{\"chat\":{\"provider\":\"vertex_ai\",\"credentialsJson\":\"" + realJson.replace("\"", "\\\"") + "\"}}";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertTrue(result.contains("service_account"));
        assertFalse(result.contains("*****"));
    }

    // -------------------------------------------------------------------------
    // merge — storedJson is valid JSON but not an object
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_storedJsonIsArray_returnsNewJsonUnchanged() {
        final String newJson    = "{\"chat\":{\"apiKey\":\"*****\"}}";
        final String storedJson = "[\"not\",\"an\",\"object\"]";

        // storedJson is valid JSON but not an object — merge is skipped to avoid
        // persisting the ***** sentinel as a real credential value
        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertEquals(newJson, result);
    }

    @Test
    public void test_merge_storedJsonIsNull_returnsNewJsonUnchanged() {
        final String newJson    = "{\"chat\":{\"apiKey\":\"*****\"}}";
        final String storedJson = "null";

        final String result = ProviderConfigMerger.merge(newJson, storedJson);

        assertEquals(newJson, result);
    }

    // -------------------------------------------------------------------------
    // merge — invalid JSON
    // -------------------------------------------------------------------------

    @Test
    public void test_merge_invalidNewJson_returnsNewJsonUnchanged() {
        final String badJson    = "not-valid-json";
        final String storedJson = "{\"chat\":{\"apiKey\":\"sk-real\"}}";

        assertEquals(badJson, ProviderConfigMerger.merge(badJson, storedJson));
    }

    @Test
    public void test_merge_invalidStoredJson_returnsNewJsonUnchanged() {
        final String newJson  = "{\"chat\":{\"apiKey\":\"*****\"}}";
        final String badStored = "not-valid-json";

        assertEquals(newJson, ProviderConfigMerger.merge(newJson, badStored));
    }

}
