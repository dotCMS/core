package com.dotcms.saml;

import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DotIdentityProviderConfigurationImplTest {

    @Test
    public void supported_signature_validation_types_are_kept() {
        assertEquals(DotSamlConstants.RESPONSE,
                DotIdentityProviderConfigurationImpl.resolveSignatureValidationType("response"));
        assertEquals(DotSamlConstants.ASSERTION,
                DotIdentityProviderConfigurationImpl.resolveSignatureValidationType(" Assertion "));
        assertEquals(DotSamlConstants.RESPONSE_AND_ASSERTION,
                DotIdentityProviderConfigurationImpl.resolveSignatureValidationType("responseandassertion"));
    }

    @Test
    public void unsupported_signature_validation_types_require_both_signatures() {
        for (final String configured : new String[]{"none", "NONE", "", "  ", null, "signature"}) {
            assertEquals(DotSamlConstants.RESPONSE_AND_ASSERTION,
                    DotIdentityProviderConfigurationImpl.resolveSignatureValidationType(configured),
                    "type '" + configured + "'");
        }
    }

    @Test
    public void stored_none_is_read_as_response_and_assertion() {
        final DotIdentityProviderConfigurationImpl config = config(secrets()
                .withSecret("signatureValidationType", "none")
                .build());

        assertEquals(DotSamlConstants.RESPONSE_AND_ASSERTION, config.getSignatureValidationType());
    }

    @Test
    public void signature_check_overrides_are_hidden_from_the_bundle() {
        final DotIdentityProviderConfigurationImpl config = config(secrets()
                .withSecret("signatureValidationType", "assertion")
                .withSecret(SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName(), "false")
                .withSecret(SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE.getPropertyName(), "false")
                .build());

        for (final SamlName ignored : new SamlName[]{SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS,
                SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE}) {
            assertFalse(config.containsOptionalProperty(ignored.getPropertyName()), ignored.getPropertyName());
            assertNull(config.getOptionalProperty(ignored.getPropertyName()), ignored.getPropertyName());
        }
    }

    @Test
    public void other_extra_parameters_still_reach_the_bundle() {
        final DotIdentityProviderConfigurationImpl config = config(secrets()
                .withSecret("signatureValidationType", "assertion")
                .withSecret("allow.unsolicited.responses", "true")
                .withSecret(SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName(), "5000")
                .build());

        assertTrue(config.containsOptionalProperty("allow.unsolicited.responses"));
        assertEquals("true", config.getOptionalProperty("allow.unsolicited.responses"));
        assertTrue(config.containsOptionalProperty(SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName()));
        assertEquals("5000", config.getOptionalProperty(SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName()));
        assertFalse(config.containsOptionalProperty("not.stored"));
    }

    @Test
    public void building_the_configuration_repeatedly_still_works_for_legacy_values() {
        // the configuration is rebuilt per request; the warning is logged once, the behaviour never changes
        final Host host = host();
        final AppSecrets legacy = secrets()
                .withSecret("signatureValidationType", "none")
                .withSecret(SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName(), "false")
                .build();

        for (int i = 0; i < 3; i++) {
            final DotIdentityProviderConfigurationImpl config = new DotIdentityProviderConfigurationImpl(null, host, legacy);
            assertEquals(DotSamlConstants.RESPONSE_AND_ASSERTION, config.getSignatureValidationType());
            assertFalse(config.containsOptionalProperty(
                    SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName()));
        }
    }

    private static AppSecrets.Builder secrets() {
        return AppSecrets.builder().withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY);
    }

    private static DotIdentityProviderConfigurationImpl config(final AppSecrets appSecrets) {
        return new DotIdentityProviderConfigurationImpl(null, host(), appSecrets);
    }

    private static Host host() {
        final Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn(UUID.randomUUID().toString());
        when(host.getHostname()).thenReturn("demo.example.com");
        return host;
    }
}
