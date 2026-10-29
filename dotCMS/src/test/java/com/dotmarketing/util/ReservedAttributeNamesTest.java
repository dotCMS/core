package com.dotmarketing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.auth.providers.oauth.OAuthConstants;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the shared set of session and request attribute names that only dotCMS itself sets.
 */
class ReservedAttributeNamesTest {

    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    /**
     * Given each attribute name dotCMS uses for the signed-in user (the user id, the user, the
     * Login As principal, the edit-mode redirect), for the Time Machine preview (date, language,
     * site) and for the OAuth and SAML login flows,
     * When it is looked up,
     * Then it is reported as reserved.
     */
    @ParameterizedTest
    @ValueSource(strings = {"USER_ID", "USER", "PRINCIPAL_USER_ID", "LOGIN_TO_EDIT_MODE",
            "tm_date", "tm_lang", "tm_host",
            OAuthConstants.SESSION_NATIVE_LOGIN, "OAUTH_REDIRECT", "OAUTH_STATE", "OAUTH_NONCE",
            "OAUTH_CODE_VERIFIER", "OAUTH_ACCESS_TOKEN", "OAUTH_ID_TOKEN", "OAUTH_PROVIDER_TYPE",
            "OAUTH_FRONT_END_LOGIN", "OAUTH_ORIGINAL_REQUEST", "original_request"})
    void signedInUserPreviewAndLoginFlowNamesAreReserved(final String name) {
        assertTrue(ReservedAttributeNames.isReserved(name));
    }

    /**
     * Given the SAML session keys that dotCMS stores with the site id in front of them (the
     * signed-in user, the session index, the name id),
     * When a name made of a site id and one of those keys is looked up,
     * Then it is reported as reserved, whatever the site id.
     */
    @ParameterizedTest
    @ValueSource(strings = {"SAMLUserId", "SAMLSessionIndex", "SAMLNameID"})
    void siteSamlKeysAreReserved(final String key) {
        assertTrue(ReservedAttributeNames.isReserved(SITE_ID + key));
        assertTrue(ReservedAttributeNames.isReserved("another-site" + key));
        assertTrue(ReservedAttributeNames.isReserved(key));
    }

    /**
     * Given names that only resemble a site SAML key, in another letter case, with trailing
     * whitespace, with more text after the key or with part of it,
     * When they are looked up,
     * Then none is reported as reserved.
     */
    @ParameterizedTest
    @ValueSource(strings = {SITE_ID + "samluserid", SITE_ID + "SAMLUserId ", "SAMLUserIdX",
            "SAMLUser", "SAMLUserId_copy"})
    void namesResemblingASiteSamlKeyAreNotReserved(final String name) {
        assertFalse(ReservedAttributeNames.isReserved(name));
    }

    /**
     * Given an ordinary attribute name, such as one a rule sets for page personalization, or a
     * reserved name in another letter case, padded or extended,
     * When it is looked up,
     * Then it is not reported as reserved.
     */
    @Test
    void ordinaryAttributeNameIsNotReserved() {
        assertFalse(ReservedAttributeNames.isReserved("visitorSegment"));
        assertFalse(ReservedAttributeNames.isReserved("userId"));
        assertFalse(ReservedAttributeNames.isReserved("user_id"));
        assertFalse(ReservedAttributeNames.isReserved(" USER_ID "));
        assertFalse(ReservedAttributeNames.isReserved("USER_ID_X"));
        assertFalse(ReservedAttributeNames.isReserved("oauth_state"));
        assertFalse(ReservedAttributeNames.isReserved(""));
        assertFalse(ReservedAttributeNames.isReserved(null));
    }

    /**
     * Given a reserved fixed name, a site SAML key and an ordinary name,
     * When the reserved name each one matches is asked for,
     * Then the fixed name and the bare SAML key are returned, never the site id, and the
     * ordinary name returns nothing.
     */
    @Test
    void reservedNameIsTheMatchedKey() {
        assertEquals(Optional.of("USER_ID"), ReservedAttributeNames.reservedName("USER_ID"));
        assertEquals(Optional.of("SAMLUserId"),
                ReservedAttributeNames.reservedName(SITE_ID + "SAMLUserId"));
        assertEquals(Optional.empty(), ReservedAttributeNames.reservedName("visitorSegment"));
        assertEquals(Optional.empty(), ReservedAttributeNames.reservedName(null));
    }
}
