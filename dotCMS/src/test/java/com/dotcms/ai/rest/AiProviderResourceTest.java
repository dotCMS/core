package com.dotcms.ai.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the masked-credential exfiltration/SSRF guard in
 * {@link AiProviderResource#testConnection}: a masked credential (e.g. {@code "apiKey":
 * "*****"}) must only resolve to the real stored secret when the posted {@code provider} and
 * {@code endpoint} match what's actually stored — otherwise a caller could pair a masked field
 * (obtainable from any {@code GET}) with an attacker-controlled {@code endpoint} and have the
 * server send the real secret there.
 */
public class AiProviderResourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -------------------------------------------------------------------------
    // resolveMaskedCredentials
    // -------------------------------------------------------------------------

    /**
     * Given a blank stored providerConfig,
     * When resolveMaskedCredentials is called,
     * Then the posted body is returned unchanged.
     */
    @Test
    public void test_resolveMaskedCredentials_blankStored_returnsBodyUnchanged() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\"}";

        assertEquals(body, AiProviderResource.resolveMaskedCredentials(body, "", "chat"));
        assertEquals(body, AiProviderResource.resolveMaskedCredentials(body, null, "chat"));
    }

    /**
     * Given a posted body with no masked sentinel,
     * When resolveMaskedCredentials is called,
     * Then the posted body is returned unchanged, even though a stored config exists.
     */
    @Test
    public void test_resolveMaskedCredentials_noMaskedValue_returnsBodyUnchanged() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"sk-real\"}";
        final String stored = "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"sk-stored\"}}";

        assertEquals(body, AiProviderResource.resolveMaskedCredentials(body, stored, "chat"));
    }

    /**
     * Given a masked apiKey and a posted provider/endpoint that match the stored section,
     * When resolveMaskedCredentials is called,
     * Then the real stored apiKey is restored.
     */
    @Test
    public void test_resolveMaskedCredentials_matchingProviderAndEndpoint_restoresCredential() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\",\"model\":\"gpt-4o\"}";
        final String stored = "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"sk-real-key\",\"model\":\"gpt-3.5\"}}";

        final String result = AiProviderResource.resolveMaskedCredentials(body, stored, "chat");

        assertTrue(result.contains("sk-real-key"));
        assertTrue(result.contains("gpt-4o")); // non-guarded field from the posted body still wins
        assertFalse(result.contains("*****"));
    }

    /**
     * Given a masked apiKey posted with a DIFFERENT provider than what's stored for that
     * capability (e.g. the UI carried over a masked value across a provider switch),
     * When resolveMaskedCredentials is called,
     * Then the sentinel is left in place rather than resolving to the wrong provider's secret.
     */
    @Test
    public void test_resolveMaskedCredentials_providerMismatch_leavesSentinelInPlace() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\"}";
        final String stored = "{\"chat\":{\"provider\":\"vertex_ai\",\"apiKey\":\"sk-real-key\"}}";

        final String result = AiProviderResource.resolveMaskedCredentials(body, stored, "chat");

        assertTrue(result.contains("*****"));
        assertFalse(result.contains("sk-real-key"));
    }

    /**
     * Given a masked apiKey posted alongside an endpoint that differs from the stored endpoint
     * (the exfiltration/SSRF attempt: pair a masked credential with an attacker-controlled host),
     * When resolveMaskedCredentials is called,
     * Then the sentinel is left in place rather than sending the real secret to the new endpoint.
     */
    @Test
    public void test_resolveMaskedCredentials_endpointMismatch_leavesSentinelInPlace() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\","
                + "\"endpoint\":\"https://attacker.example/v1\"}";
        final String stored = "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"sk-real-key\","
                + "\"endpoint\":\"https://api.openai.com/v1\"}}";

        final String result = AiProviderResource.resolveMaskedCredentials(body, stored, "chat");

        assertTrue(result.contains("*****"));
        assertFalse(result.contains("sk-real-key"));
    }

    /**
     * Given a masked apiKey where neither the posted body nor the stored config sets an endpoint
     * (the common case — no custom endpoint override),
     * When resolveMaskedCredentials is called,
     * Then the credential still resolves, since a missing endpoint on both sides is a match.
     */
    @Test
    public void test_resolveMaskedCredentials_neitherHasEndpoint_stillResolves() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\"}";
        final String stored = "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"sk-real-key\"}}";

        final String result = AiProviderResource.resolveMaskedCredentials(body, stored, "chat");

        assertTrue(result.contains("sk-real-key"));
    }

    /**
     * Given a stored providerConfig with no section for the requested capability,
     * When resolveMaskedCredentials is called,
     * Then the posted body is returned unchanged.
     */
    @Test
    public void test_resolveMaskedCredentials_missingSection_returnsBodyUnchanged() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\"}";
        final String stored = "{\"embeddings\":{\"provider\":\"openai\",\"apiKey\":\"sk-real-key\"}}";

        final String result = AiProviderResource.resolveMaskedCredentials(body, stored, "chat");

        assertTrue(result.contains("*****"));
    }

    /**
     * Given a stored section that isn't a JSON object (defensively malformed data),
     * When resolveMaskedCredentials is called,
     * Then the posted body is returned unchanged.
     */
    @Test
    public void test_resolveMaskedCredentials_sectionNotAnObject_returnsBodyUnchanged() {
        final String body = "{\"provider\":\"openai\",\"apiKey\":\"*****\"}";
        final String stored = "{\"chat\":\"not-an-object\"}";

        assertEquals(body, AiProviderResource.resolveMaskedCredentials(body, stored, "chat"));
    }

    /**
     * Given a posted body that isn't valid JSON,
     * When resolveMaskedCredentials is called,
     * Then the posted body is returned unchanged rather than throwing.
     */
    @Test
    public void test_resolveMaskedCredentials_invalidBody_returnsBodyUnchanged() {
        final String body = "not-valid-json-*****";
        final String stored = "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"sk-real-key\"}}";

        assertEquals(body, AiProviderResource.resolveMaskedCredentials(body, stored, "chat"));
    }

    // -------------------------------------------------------------------------
    // targetsStoredDestination
    // -------------------------------------------------------------------------

    /**
     * Given incoming and stored nodes with the same provider and endpoint,
     * When targetsStoredDestination is called,
     * Then it returns true.
     */
    @Test
    public void test_targetsStoredDestination_matchingProviderAndEndpoint_returnsTrue() throws Exception {
        final JsonNode incoming = node("{\"provider\":\"openai\",\"endpoint\":\"https://api.openai.com/v1\"}");
        final JsonNode stored = node("{\"provider\":\"openai\",\"endpoint\":\"https://api.openai.com/v1\"}");

        assertTrue(AiProviderResource.targetsStoredDestination(incoming, stored));
    }

    /**
     * Given incoming and stored nodes with different providers,
     * When targetsStoredDestination is called,
     * Then it returns false.
     */
    @Test
    public void test_targetsStoredDestination_differentProvider_returnsFalse() throws Exception {
        final JsonNode incoming = node("{\"provider\":\"openai\"}");
        final JsonNode stored = node("{\"provider\":\"vertex_ai\"}");

        assertFalse(AiProviderResource.targetsStoredDestination(incoming, stored));
    }

    /**
     * Given incoming and stored nodes with the same provider but different endpoints,
     * When targetsStoredDestination is called,
     * Then it returns false.
     */
    @Test
    public void test_targetsStoredDestination_differentEndpoint_returnsFalse() throws Exception {
        final JsonNode incoming = node("{\"provider\":\"openai\",\"endpoint\":\"https://attacker.example\"}");
        final JsonNode stored = node("{\"provider\":\"openai\",\"endpoint\":\"https://api.openai.com/v1\"}");

        assertFalse(AiProviderResource.targetsStoredDestination(incoming, stored));
    }

    /**
     * Given incoming and stored nodes where neither sets an endpoint,
     * When targetsStoredDestination is called,
     * Then it returns true, since a missing endpoint on both sides matches.
     */
    @Test
    public void test_targetsStoredDestination_neitherHasEndpoint_returnsTrue() throws Exception {
        final JsonNode incoming = node("{\"provider\":\"openai\"}");
        final JsonNode stored = node("{\"provider\":\"openai\"}");

        assertTrue(AiProviderResource.targetsStoredDestination(incoming, stored));
    }

    /**
     * Given an incoming node that sets an endpoint while the stored node has none,
     * When targetsStoredDestination is called,
     * Then it returns false, since that's exactly the "point the secret at a new host" case.
     */
    @Test
    public void test_targetsStoredDestination_onlyIncomingHasEndpoint_returnsFalse() throws Exception {
        final JsonNode incoming = node("{\"provider\":\"openai\",\"endpoint\":\"https://attacker.example\"}");
        final JsonNode stored = node("{\"provider\":\"openai\"}");

        assertFalse(AiProviderResource.targetsStoredDestination(incoming, stored));
    }

    // -------------------------------------------------------------------------
    // textEquals
    // -------------------------------------------------------------------------

    /**
     * Given two nodes with equal text values,
     * When textEquals is called,
     * Then it returns true.
     */
    @Test
    public void test_textEquals_equalValues_returnsTrue() throws Exception {
        final JsonNode a = node("{\"v\":\"openai\"}").get("v");
        final JsonNode b = node("{\"v\":\"openai\"}").get("v");

        assertTrue(AiProviderResource.textEquals(a, b));
    }

    /**
     * Given two nodes with different text values,
     * When textEquals is called,
     * Then it returns false.
     */
    @Test
    public void test_textEquals_differentValues_returnsFalse() throws Exception {
        final JsonNode a = node("{\"v\":\"openai\"}").get("v");
        final JsonNode b = node("{\"v\":\"vertex_ai\"}").get("v");

        assertFalse(AiProviderResource.textEquals(a, b));
    }

    /**
     * Given both nodes are null (field absent on both sides),
     * When textEquals is called,
     * Then it returns true.
     */
    @Test
    public void test_textEquals_bothNull_returnsTrue() {
        assertTrue(AiProviderResource.textEquals(null, null));
    }

    /**
     * Given one node is null (field absent) and the other holds a value,
     * When textEquals is called,
     * Then it returns false.
     */
    @Test
    public void test_textEquals_oneNull_returnsFalse() throws Exception {
        final JsonNode b = node("{\"v\":\"openai\"}").get("v");

        assertFalse(AiProviderResource.textEquals(null, b));
        assertFalse(AiProviderResource.textEquals(b, null));
    }

    /**
     * Given both nodes are the JSON null literal (field present but explicitly null),
     * When textEquals is called,
     * Then it returns true, since a JSON null is treated the same as an absent field.
     */
    @Test
    public void test_textEquals_bothJsonNullLiteral_returnsTrue() throws Exception {
        final JsonNode a = node("{\"v\":null}").get("v");
        final JsonNode b = node("{\"v\":null}").get("v");

        assertTrue(AiProviderResource.textEquals(a, b));
    }

    private static JsonNode node(final String json) throws Exception {
        return MAPPER.readTree(json);
    }

}
