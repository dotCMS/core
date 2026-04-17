package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Sanity checks on {@link OAuthConstants} — primarily verifying that the core
 * OAuth endpoints deliberately differ from the legacy plugin endpoints so that
 * both can coexist.
 */
class OAuthConstantsTest {

    @Test
    void corePaths_deliberatelyDifferFromLegacyPluginPaths() {
        // Plugin used /api/v1/oauth2/callback; core uses /api/v1/oauth/callback
        assertNotEquals("/api/v1/oauth2/callback", OAuthConstants.CALLBACK_PATH);
        assertEquals("/api/v1/oauth/callback", OAuthConstants.CALLBACK_PATH);

        // Plugin used dotOAuthApp; core uses dotOAuth
        assertNotEquals("dotOAuthApp", OAuthConstants.APP_KEY);
        assertEquals("dotOAuth", OAuthConstants.APP_KEY);
    }

    @Test
    void logoutPaths_coverAllExpectedEntryPoints() {
        assertTrue(Arrays.asList(OAuthConstants.LOGOUT_PATHS).contains("/api/v1/logout"));
        assertTrue(Arrays.asList(OAuthConstants.LOGOUT_PATHS).contains("/dotCMS/logout"));
        assertTrue(Arrays.asList(OAuthConstants.LOGOUT_PATHS).contains("/dotAdmin/logout"));
    }
}
