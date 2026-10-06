package com.dotcms.rest.api.v1.maintenance;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SessionTokenUtil}. Verifies that the REST migration of
 * the Logged Users HMAC scheme produces byte-for-byte identical tokens to the
 * legacy DWR implementation in {@code UserSessionAjax}, so existing clients see
 * no regression in token shape or length.
 *
 * @author hassandotcms
 */
class SessionTokenUtilTest {

    private static final String SESSION_ID = "ABC123sessionId";
    private static final String SECRET = "csrf-secret";

    /**
     * Given scenario: same inputs hashed twice
     * Expected result: deterministic, identical output
     */
    @Test
    void obfuscateSessionId_isDeterministic() {
        final String first = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        final String second = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        assertEquals(first, second);
    }

    /**
     * Given scenario: 16-byte truncated HMAC-SHA256 Base64-URL encoded without padding
     * Expected result: exactly 22 characters, no '=' padding, only URL-safe Base64 alphabet
     */
    @Test
    void obfuscateSessionId_producesUrlSafeBase64Of16Bytes() {
        final String token = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        assertEquals(22, token.length(),
                "16 bytes Base64 URL-encoded without padding is exactly 22 chars");
        assertFalse(token.contains("="), "no padding expected");
        assertTrue(token.matches("[A-Za-z0-9_-]+"),
                "tokens must use the URL-safe Base64 alphabet");
    }

    /**
     * Given scenario: REST {@link SessionTokenUtil} and the legacy DWR-era reference
     *                 implementation are run on the same inputs
     * Expected result: identical tokens — guarantees the refactor is byte-for-byte
     *                  compatible with any persisted/streamed values
     */
    @Test
    void obfuscateSessionId_matchesLegacyReferenceImplementation() throws Exception {
        final String legacy = legacyObfuscate(SESSION_ID, SECRET);
        final String modern = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        assertEquals(legacy, modern,
                "REST utility must produce the same token as the original UserSessionAjax code");
    }

    /**
     * Given scenario: a token computed under secret A is validated under secret B
     * Expected result: validation fails — the CSRF binding is enforced
     */
    @Test
    void validateSessionId_rejectsTokenComputedUnderDifferentSecret() {
        final String token = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        assertFalse(SessionTokenUtil.validateSessionId(SESSION_ID, "different-secret", token));
    }

    /**
     * Given scenario: a token for session A is checked against session B
     * Expected result: validation fails
     */
    @Test
    void validateSessionId_rejectsTokenForDifferentSession() {
        final String token = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        assertFalse(SessionTokenUtil.validateSessionId("other-session-id", SECRET, token));
    }

    /**
     * Given scenario: matching session id, secret and token
     * Expected result: validation succeeds
     */
    @Test
    void validateSessionId_acceptsMatchingToken() {
        final String token = SessionTokenUtil.obfuscateSessionId(SESSION_ID, SECRET);
        assertTrue(SessionTokenUtil.validateSessionId(SESSION_ID, SECRET, token));
    }

    /**
     * Given scenario: null token
     * Expected result: validation rejects without NPE — defensive against bad inputs
     */
    @Test
    void validateSessionId_rejectsNullToken() {
        assertFalse(SessionTokenUtil.validateSessionId(SESSION_ID, SECRET, null));
    }

    /**
     * Reference implementation copied verbatim from the pre-refactor
     * {@code UserSessionAjax#obfuscateSessionId}. Lives only in this test to pin the
     * wire format. If a future refactor changes the output, this test will fail.
     */
    private static String legacyObfuscate(final String sessionId, final String secretKey)
            throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        final SecretKeySpec key = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(key);
        final byte[] hash = mac.doFinal(sessionId.getBytes(StandardCharsets.UTF_8));
        final byte[] truncatedHash = new byte[16];
        System.arraycopy(hash, 0, truncatedHash, 0, 16);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(truncatedHash);
    }
}
