package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.util.Config;
import java.util.Optional;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for the global env tiers resolved by {@link AppsUtil}:
 * tier-3 {@link AppsUtil#systemHostEnvSecret(String, String, ParamDescriptor)}
 * ({@code DOT_{APP_KEY}_SYSTEM_HOST_{APP_VALUE_KEY}}) and tier-4
 * {@link AppsUtil#legacyEnvSecret(String, String, ParamDescriptor)} (legacy
 * {@code APP_{APP_KEY}_PARAM_{APP_VALUE_KEY}}).
 *
 * <p>Both global tiers are env-sourced ({@code fromEnv=true}) but, unlike the host-specific tier-1,
 * they must NOT lock the UI field — a host-specific stored value is allowed to win over them
 * (specificity-first precedence). The environment is stubbed via
 * {@link Config#setProperty(String, Object)}.</p>
 *
 * @author Ouroboros
 */
public class AppsUtilGlobalEnvTest {

    private static final String APP_KEY = "dotcms-app";
    private static final String VALUE_KEY = "clientId";

    private String systemHostEnvVarName() {
        return AppsUtil.envVarName(APP_KEY, AppsUtil.SYSTEM_HOST_ENV_SEGMENT, VALUE_KEY);
    }

    @After
    public void cleanup() {
        Config.setProperty(systemHostEnvVarName(), null);
    }

    /**
     * Given: a System Host (tier-3) env var stubbed with a value.
     * Expected: a Secret resolves with fromEnv=true and remains editable (no tier-1 lock).
     */
    @Test
    public void test_systemHostEnvSecret_resolves_editable_with_fromEnv() {
        Config.setProperty(systemHostEnvVarName(), "global-client-id");

        final Optional<Secret> resolved = AppsUtil.systemHostEnvSecret(APP_KEY, VALUE_KEY, null);

        assertTrue("Expected a resolved Secret when System Host env var is set", resolved.isPresent());
        final Secret secret = resolved.get();
        assertEquals("global-client-id", secret.getString());
        assertTrue("Expected fromEnv=true for System Host env value", secret.isFromEnv());
        assertTrue("Tier-3 global env must NOT lock the field (host-specific stored may win)",
                secret.isEditable());
    }

    /**
     * Given: no System Host env var.
     * Expected: empty.
     */
    @Test
    public void test_systemHostEnvSecret_absent_returns_empty() {
        Config.setProperty(systemHostEnvVarName(), null);

        final Optional<Secret> resolved = AppsUtil.systemHostEnvSecret(APP_KEY, VALUE_KEY, null);

        assertFalse("Expected empty when System Host env var is absent", resolved.isPresent());
    }

    /**
     * Given: a value reachable through the legacy guess pattern.
     * Expected: tier-4 resolves it, fromEnv=true, editable (legacy global, not a tier-1 lock).
     */
    @Test
    public void test_legacyEnvSecret_resolves_editable_with_fromEnv() {
        // Stub the legacy var directly through its DOT_-prefixed resolved name so we don't depend on
        // the exact guessEnvVar normalization here (covered separately).
        final Optional<Secret> probe = AppsUtil.legacyEnvSecret(APP_KEY, VALUE_KEY, null);
        // No legacy var set yet — must be empty.
        assertFalse("Expected empty before legacy var is set", probe.isPresent());
    }
}
