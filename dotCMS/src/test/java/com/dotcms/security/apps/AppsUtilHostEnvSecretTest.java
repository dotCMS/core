package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link AppsUtil#hostEnvSecret(String, String, String)} which performs the
 * tier-1 host-specific environment variable lookup: it constructs the env var name via
 * {@code AppsUtil.envVarName()}, reads it through {@code Config.getStringProperty()} and returns
 * the resolved value as a {@link Secret} with {@code fromEnv=true}. An absent or empty env var
 * resolves to no value.
 *
 * <p>The environment is stubbed via {@link Config#setProperty(String, Object)} so the lookup is
 * exercised without touching real process environment variables.</p>
 *
 * @author Ouroboros
 */
public class AppsUtilHostEnvSecretTest {

    private static final String APP_KEY = "dotcms-app";
    private static final String HOST_NAME = "demo.dotcms.com";
    private static final String VALUE_KEY = "clientId";

    private String envVarName() {
        return AppsUtil.envVarName(APP_KEY, HOST_NAME, VALUE_KEY);
    }

    @After
    public void cleanup() {
        // Clear the stubbed env var so tests do not leak state into each other.
        Config.setProperty(envVarName(), null);
    }

    /**
     * Given: a host-specific env var stubbed with a value.
     * Expected: a Secret is resolved carrying the value with fromEnv=true.
     */
    @Test
    public void test_hostEnvSecret_resolves_value_with_fromEnv_true() {
        Config.setProperty(envVarName(), "the-client-id");

        final Optional<Secret> resolved = AppsUtil.hostEnvSecret(APP_KEY, HOST_NAME, VALUE_KEY, null);

        assertTrue("Expected a resolved Secret when env var is set", resolved.isPresent());
        final Secret secret = resolved.get();
        assertEquals("the-client-id", secret.getString());
        assertTrue("Expected fromEnv flag to be true for env-sourced value", secret.isFromEnv());
        assertFalse("Tier-1 host-specific env must lock the UI field (read-only)",
                secret.isEditable());
    }

    /**
     * Given: no env var set for the constructed host-specific name.
     * Expected: no value is returned.
     */
    @Test
    public void test_hostEnvSecret_absent_env_returns_empty() {
        // Ensure absent.
        Config.setProperty(envVarName(), null);

        final Optional<Secret> resolved = AppsUtil.hostEnvSecret(APP_KEY, HOST_NAME, VALUE_KEY, null);

        assertFalse("Expected no Secret when env var is absent", resolved.isPresent());
    }

    /**
     * Given: an empty-string env var.
     * Expected: no value is returned (empty env is treated as not set).
     */
    @Test
    public void test_hostEnvSecret_empty_env_returns_empty() {
        Config.setProperty(envVarName(), "");

        final Optional<Secret> resolved = AppsUtil.hostEnvSecret(APP_KEY, HOST_NAME, VALUE_KEY, null);

        assertFalse("Expected no Secret when env var is empty", resolved.isPresent());
    }
}
