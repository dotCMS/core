package com.dotcms.ai.app;

import com.dotcms.security.apps.Secret;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the \AIAppUtil\ class. This test class verifies the functionality
 * of methods in \AIAppUtil\ such as discovering secrets, creating models, and resolving
 * environment-specific secrets. It uses mock objects to simulate the \Secret\ dependencies.
 *
 * @author vico
 */
public class AIAppUtilTest {

    private AIAppUtil aiAppUtil;
    private Map<String, Secret> secrets;
    private Secret secret;

    @Before
    public void setUp() {
        aiAppUtil = AIAppUtil.get();
        secrets = mock(Map.class);
        secret = mock(Secret.class);
    }

    /**
     * Given a map of secrets containing a key with a secret value
     * When the discoverSecret method is called with the key and a default value
     * Then the secret value should be returned.
     */
    @Test
    public void testDiscoverSecretWithDefaultValue() {
        when(secrets.get("apiKey")).thenReturn(secret);
        when(secret.getString()).thenReturn("secretValue");

        String result = aiAppUtil.discoverSecret(secrets, AppKeys.API_KEY, "defaultValue");
        assertEquals("secretValue", result);
    }

    /**
     * Given a map of secrets not containing a key
     * When the discoverSecret method is called with the key and a default value
     * Then the default value should be returned.
     */
    @Test
    public void testDiscoverSecretWithDefaultValueNotFound() {
        when(secrets.get("key")).thenReturn(null);

        String result = aiAppUtil.discoverSecret(secrets, AppKeys.API_KEY, "defaultValue");
        assertEquals("defaultValue", result);
    }

    /**
     * Given a map of secrets containing a key with a secret value
     * When the discoverSecret method is called with the key
     * Then the secret value should be returned.
     */
    @Test
    public void testDiscoverSecretWithKeyDefaultValue() {
        when(secrets.get("apiKey")).thenReturn(secret);
        when(secret.getString()).thenReturn("secretValue");

        String result = aiAppUtil.discoverSecret(secrets, AppKeys.API_KEY);
        assertEquals("secretValue", result);
    }

    /**
     * Given a map of secrets not containing a key
     * When the discoverSecret method is called with the key
     * Then the default value of the key should be returned.
     */
    @Test
    public void testDiscoverSecretWithKeyDefaultValueNotFound() {
        when(secrets.get("key")).thenReturn(null);

        String result = aiAppUtil.discoverSecret(secrets, AppKeys.API_KEY);
        assertEquals(AppKeys.API_KEY.defaultValue, result);
    }

    /**
     * Given a map of secrets containing a key with an integer secret value
     * When the discoverIntSecret method is called with the key
     * Then the integer secret value should be returned.
     */
    @Test
    public void testDiscoverIntSecret() {
        when(secrets.get("apiKey")).thenReturn(secret);
        when(secret.getString()).thenReturn("123");

        int result = aiAppUtil.discoverIntSecret(secrets, AppKeys.API_KEY);
        assertEquals(123, result);
    }

    /**
     * Given a map of secrets containing a key with a boolean secret value
     * When the discoverBooleanSecret method is called with the key
     * Then the boolean secret value should be returned.
     */
    @Test
    public void testDiscoverBooleanSecret() {
        when(secrets.get("apiKey")).thenReturn(secret);
        when(secret.getString()).thenReturn("true");

        boolean result = aiAppUtil.discoverBooleanSecret(secrets, AppKeys.API_KEY);
        assertTrue(result);
    }

    @Test
    public void testDiscoverEnvSecret() {
        // Mock the secret value in the secrets map
        when(secrets.get("apiKey")).thenReturn(secret);
        when(secret.getString()).thenReturn("secretValue");

        // Call the method with the key and environment variable
        String result = aiAppUtil.discoverEnvSecret(secrets, AppKeys.API_KEY, "ENV_API_KEY");

        // Assert the expected outcome
        assertEquals("secretValue", result);
    }

}