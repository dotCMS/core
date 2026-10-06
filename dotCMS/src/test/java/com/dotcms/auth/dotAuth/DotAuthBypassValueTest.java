package com.dotcms.auth.dotAuth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import com.dotcms.filters.interceptor.saml.SamlWebUtils;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * OAuth/OIDC and SAML must gate the native-login bypass on the same token, so hardening it
 * away from the guessable {@code true} default covers every dotAuth protocol at once.
 */
class DotAuthBypassValueTest {

    @AfterEach
    void resetProperties() {
        Config.setProperty("SAML_BYPASS_VALUE", null);
        Config.setProperty("SSO_BYPASS_TOKEN", null);
    }

    @Test
    void bothProtocolsReadTheSameParamAndDefaultValue() {
        assertEquals(SamlWebUtils.BY_PASS_KEY, OAuthConstants.PARAM_NATIVE);
        assertEquals("true", DotAuthConstants.getBypassValue());
    }

    @Test
    void protocolNeutralTokenOverridesTheDefault() {
        Config.setProperty("SSO_BYPASS_TOKEN", "s3cret");
        assertEquals("s3cret", DotAuthConstants.getBypassValue());
    }

    @Test
    void legacySamlPropertyWinsForBackwardsCompatibility() {
        Config.setProperty("SSO_BYPASS_TOKEN", "s3cret");
        Config.setProperty("SAML_BYPASS_VALUE", "legacy");
        assertEquals("legacy", DotAuthConstants.getBypassValue());
    }
}
