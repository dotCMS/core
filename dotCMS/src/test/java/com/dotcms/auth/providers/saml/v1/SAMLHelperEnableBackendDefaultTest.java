package com.dotcms.auth.providers.saml.v1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.saml.IdentityProviderConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Pins the {@code enableBackend} default: absent flag must NOT grant the back-end role
 * on provisioning — matching the pre-dotAuth behavior on main, where handleRoles never
 * granted {@code DOTCMS_BACK_END_USER} implicitly. The flag is an explicit opt-in.
 */
class SAMLHelperEnableBackendDefaultTest {

    private static IdentityProviderConfiguration idp(final boolean hasFlag, final Object value) {
        final IdentityProviderConfiguration cfg = mock(IdentityProviderConfiguration.class);
        when(cfg.containsOptionalProperty("enableBackend")).thenReturn(hasFlag);
        if (hasFlag) {
            when(cfg.getOptionalProperty("enableBackend")).thenReturn(value);
        }
        return cfg;
    }

    @Test
    void absentFlag_defaultsClosed_matchingMainBehavior() {
        assertFalse(SAMLHelper.isBackEndEnabled(idp(false, null)),
                "No enableBackend flag must not grant the back-end role (main never did)");
    }

    @Test
    void explicitTrue_grants() {
        assertTrue(SAMLHelper.isBackEndEnabled(idp(true, "true")));
    }

    @Test
    void explicitFalse_denies() {
        assertFalse(SAMLHelper.isBackEndEnabled(idp(true, "false")));
    }
}
