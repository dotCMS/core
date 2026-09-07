package com.dotcms.security.apps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link AppsUtil#envVarName(String, String, String)} which builds the
 * host-specific environment variable name following the
 * {@code DOT_{APP_KEY}_{HOSTNAME}_{APP_VALUE_KEY}} convention by reusing
 * {@code Config.envKey()} normalization.
 *
 * @author Ouroboros
 */
public class AppsUtilEnvKeyTest {

    /**
     * Given: an app key, hostname and value key using dashes and dotted hostname
     * Expected: dashes and dots are normalized to underscores, all uppercased and DOT_ prefixed.
     */
    @Test
    public void test_envVarName_normalizes_dashes_and_dotted_hostname() {
        assertEquals(
                "DOT_DOTCMS_APP_DEMO_DOTCMS_COM_CLIENTID",
                AppsUtil.envVarName("dotcms-app", "demo.dotcms.com", "clientId"));
    }

    /**
     * Given: an app key with a dot and a value key with a dash
     * Expected: separators collapse to single underscores.
     */
    @Test
    public void test_envVarName_normalizes_mixed_separators() {
        assertEquals(
                "DOT_MY_APP_SYSTEM_HOST_API_KEY",
                AppsUtil.envVarName("my.app", "system_host", "api-key"));
    }

    /**
     * Given: doubled separators that produce doubled underscores
     * Expected: doubled underscores are collapsed to a single underscore.
     */
    @Test
    public void test_envVarName_collapses_doubled_underscores() {
        assertEquals(
                "DOT_FOO_BAR_HOST_KEY",
                AppsUtil.envVarName("foo--bar", "host", "key"));
    }

    /**
     * Given: a value key ending with a separator
     * Expected: the trailing underscore is trimmed.
     */
    @Test
    public void test_envVarName_trims_trailing_underscore() {
        assertEquals(
                "DOT_APP_HOST_KEY",
                AppsUtil.envVarName("app", "host", "key-"));
    }
}
