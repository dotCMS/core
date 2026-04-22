package com.dotcms.auth.dotAuth.rest.handler;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.DotAuthProtocol;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.security.apps.AppSecrets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OAuthProtocolHandlerTest {

    private final OAuthProtocolHandler handler = new OAuthProtocolHandler();

    @Test
    public void protocol_is_OAUTH() {
        assertEquals(DotAuthProtocol.OAUTH, handler.protocol());
    }

    @Test
    public void appKey_is_dotAuth() {
        assertEquals(DotAuthConstants.APP_KEY, handler.appKey());
    }

    @Test
    public void secretKeys_include_all_OAuthAppConfig_keys() {
        assertTrue(handler.secretKeys().contains(OAuthAppConfig.KEY_CLIENT_SECRET));
        assertTrue(handler.secretKeys().contains(OAuthAppConfig.KEY_ISSUER_URL));
        assertTrue(handler.secretKeys().contains(OAuthAppConfig.KEY_CALLBACK_URL));
    }

    @Test
    public void clientSecret_is_hidden() {
        assertTrue(handler.hiddenKeys().contains(OAuthAppConfig.KEY_CLIENT_SECRET));
    }

    @Test
    public void enabled_is_boolean() {
        assertTrue(handler.booleanKeys().contains(OAuthAppConfig.KEY_ENABLED));
    }

    @Test
    public void maskedValues_replaces_clientSecret_with_mask() {
        final AppSecrets secrets = AppSecrets.builder()
                .withKey(DotAuthConstants.APP_KEY)
                .withHiddenSecret(OAuthAppConfig.KEY_CLIENT_SECRET, "real-secret")
                .withSecret(OAuthAppConfig.KEY_CLIENT_ID, "my-id")
                .build();

        final Map<String, Object> out = handler.maskedValues(secrets);

        assertEquals("****", out.get(OAuthAppConfig.KEY_CLIENT_SECRET));
        assertEquals("my-id", out.get(OAuthAppConfig.KEY_CLIENT_ID));
    }

    @Test
    public void buildSecrets_preserves_stored_clientSecret_when_mask_posted_back() {
        final AppSecrets existing = AppSecrets.builder()
                .withKey(DotAuthConstants.APP_KEY)
                .withHiddenSecret(OAuthAppConfig.KEY_CLIENT_SECRET, "stored-secret")
                .build();

        final AppSecrets result = handler.buildSecrets(
                Map.of(OAuthAppConfig.KEY_CLIENT_SECRET, "****"),
                Optional.of(existing));

        assertEquals("stored-secret",
                result.getSecrets().get(OAuthAppConfig.KEY_CLIENT_SECRET).getString());
    }
}
