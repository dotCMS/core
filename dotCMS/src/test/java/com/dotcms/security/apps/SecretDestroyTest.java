package com.dotcms.security.apps;

import org.junit.Test;

/**
 * Regression tests for {@link Secret#destroy()} against env-backed Secrets.
 *
 * <p>A tier-1 (host-specific env) Secret is built with its env-var value set and the underlying
 * {@code value} char[] left {@code null}. {@code destroy()} must null-guard both arrays so it does
 * not throw an {@link NullPointerException} when invoked async via the secret-saved event.</p>
 *
 * @author Ouroboros
 */
public class SecretDestroyTest {

    /**
     * A tier-1 env-backed Secret has only envVarValue set (value is null). destroy() must not NPE.
     */
    @Test
    public void test_destroy_envValue_only_does_not_npe() {
        final Secret secret = Secret.builder()
                .withType(Type.STRING)
                .withEnvValue("from-env")
                .withFromEnv(true)
                .build();

        secret.destroy(); // must not throw
    }

    /**
     * A normal stored Secret has only value set (envVarValue null). destroy() must not NPE.
     */
    @Test
    public void test_destroy_value_only_does_not_npe() {
        final Secret secret = Secret.builder()
                .withType(Type.STRING)
                .withValue("stored")
                .build();

        secret.destroy(); // must not throw
    }
}
