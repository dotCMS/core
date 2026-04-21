package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecrets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class SamlProtocolHandlerTest {

    private final SamlProtocolHandler handler = new SamlProtocolHandler();

    @Test
    public void protocol_is_SAML() {
        assertEquals(DotAuthProtocol.SAML, handler.protocol());
    }

    @Test
    public void appKey_matches_DotSamlProxyFactory_constant() {
        assertEquals(DotSamlProxyFactory.SAML_APP_CONFIG_KEY, handler.appKey());
    }

    @Test
    public void secretKeys_match_dotsaml_config_yml_params() {
        assertEquals(
                List.of("enable", "idpName", "sPIssuerURL", "sPEndpointHostname",
                        "signatureValidationType", "idPMetadataFile", "publicCert",
                        "privateKey", "buttonParam"),
                handler.secretKeys());
    }

    @Test
    public void privateKey_is_hidden() {
        assertTrue(handler.hiddenKeys().contains("privateKey"));
    }

    @Test
    public void enable_is_boolean() {
        assertTrue(handler.booleanKeys().contains("enable"));
    }

    @Test
    public void maskedValues_replaces_privateKey_with_mask_and_unboxes_enable() {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY)
                .withHiddenSecret("privateKey", "PEM-content")
                .withSecret("enable", true)
                .withSecret("idpName", "Okta")
                .build();

        final Map<String, Object> out = handler.maskedValues(secrets);

        assertEquals("****", out.get("privateKey"));
        assertEquals(Boolean.TRUE, out.get("enable"));
        assertEquals("Okta", out.get("idpName"));
    }

    @Test
    public void buildSecrets_preserves_stored_privateKey_when_mask_posted_back() {
        final AppSecrets existing = AppSecrets.builder()
                .withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY)
                .withHiddenSecret("privateKey", "stored-PEM")
                .build();

        final AppSecrets result = handler.buildSecrets(
                Map.of("privateKey", "****", "idpName", "Okta"),
                Optional.of(existing));

        assertEquals("stored-PEM", result.getSecrets().get("privateKey").getString());
        assertEquals("Okta", result.getSecrets().get("idpName").getString());
    }

    @Test
    public void maskedValues_passes_through_undeclared_keys_as_strings() {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY)
                .withSecret("enable", true)
                .withSecret("emailAttribute", "emailAddress")
                .withSecret("rolesAttribute", "groups")
                .build();

        final Map<String, Object> out = handler.maskedValues(secrets);

        assertEquals("emailAddress", out.get("emailAttribute"));
        assertEquals("groups", out.get("rolesAttribute"));
    }

    @Test
    public void buildSecrets_writes_undeclared_keys_as_strings() {
        final AppSecrets result = handler.buildSecrets(
                Map.of(
                        "idpName", "Okta",
                        "emailAttribute", "emailAddress",
                        "autoCreateUsers", "true"),
                Optional.empty());

        assertEquals("emailAddress", result.getSecrets().get("emailAttribute").getString());
        assertEquals("true", result.getSecrets().get("autoCreateUsers").getString());
    }

    @Test
    public void buildSecrets_drops_undeclared_keys_with_empty_value() {
        final AppSecrets result = handler.buildSecrets(
                Map.of("idpName", "Okta", "emailAttribute", ""),
                Optional.empty());

        assertFalse(result.getSecrets().containsKey("emailAttribute"));
    }

    @Test
    public void buildSecrets_omits_previously_stored_undeclared_keys_not_resent() {
        // Client is authoritative: if it doesn't send a previously-stored
        // extra, we drop it. Matches how admins remove rows through the UI.
        final AppSecrets existing = AppSecrets.builder()
                .withKey(DotSamlProxyFactory.SAML_APP_CONFIG_KEY)
                .withSecret("emailAttribute", "emailAddress")
                .build();

        final AppSecrets result = handler.buildSecrets(
                Map.of("idpName", "Okta"),
                Optional.of(existing));

        assertFalse(result.getSecrets().containsKey("emailAttribute"));
    }
}
