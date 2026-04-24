package com.dotcms.auth.providers.oauth.provider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OIDCProvider#verifyIdTokenClaims} — the post-signature
 * claim-set checks (iss / aud / exp / nonce / sub). Signature verification and the
 * JWKS round-trip live in the public method; this test exercises the claim logic
 * in isolation.
 *
 * <p>The iss-with-trailing-slash case is the regression test for the bug where a
 * one-sided normalization (constructor only) rejected valid tokens emitted by
 * Auth0 / Azure B2C tenants that include the slash.
 */
class OIDCProviderClaimValidationTest {

    private static final String ISSUER    = "https://issuer.example.com";
    private static final String CLIENT_ID = "client-123";
    private static final String NONCE     = "nonce-abc";
    private static final String SUBJECT   = "user-42";

    private static JWTClaimsSet.Builder validBuilder() {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(CLIENT_ID))
                .expirationTime(new Date(System.currentTimeMillis() + 60_000L))
                .claim("nonce", NONCE)
                .subject(SUBJECT);
    }

    @Test
    void happyPath_passes() {
        assertDoesNotThrow(() ->
                OIDCProvider.verifyIdTokenClaims(validBuilder().build(), ISSUER, CLIENT_ID, NONCE));
    }

    @Test
    void issWithTrailingSlash_matchesConfiguredIssuerWithoutSlash() {
        // H-1 regression: the configured issuer is stripped of its trailing slash on
        // construction; the token's iss must be normalized the same way before the
        // equality check or validly-issued tokens from some IdPs get rejected.
        final JWTClaimsSet claims = validBuilder().issuer(ISSUER + "/").build();
        assertDoesNotThrow(() ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
    }

    @Test
    void issWrongIssuer_throws() {
        final JWTClaimsSet claims = validBuilder().issuer("https://attacker.example").build();
        final DotRuntimeException ex = assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
        assertTrue(ex.getMessage().contains("iss mismatch"), ex.getMessage());
    }

    @Test
    void audDoesNotContainClientId_throws() {
        final JWTClaimsSet claims = validBuilder().audience(List.of("some-other-client")).build();
        final DotRuntimeException ex = assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
        assertTrue(ex.getMessage().contains("aud"), ex.getMessage());
    }

    @Test
    void audContainsClientIdAmongMultiple_passes() {
        // aud is per spec either a single string or an array. Multi-aud must still pass
        // as long as our client_id is in the list.
        final JWTClaimsSet claims = validBuilder()
                .audience(List.of("other-client", CLIENT_ID, "third-client"))
                .build();
        assertDoesNotThrow(() ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
    }

    @Test
    void audMissing_throws() {
        final JWTClaimsSet claims = validBuilder().audience((List<String>) null).build();
        assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
    }

    @Test
    void expInThePast_throws() {
        final JWTClaimsSet claims = validBuilder()
                .expirationTime(new Date(System.currentTimeMillis() - 60_000L))
                .build();
        final DotRuntimeException ex = assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
        assertTrue(ex.getMessage().contains("expired"), ex.getMessage());
    }

    @Test
    void expMissing_throws() {
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(CLIENT_ID))
                .claim("nonce", NONCE)
                .subject(SUBJECT)
                .build();
        assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
    }

    @Test
    void nonceMismatch_throws() {
        final JWTClaimsSet claims = validBuilder().claim("nonce", "different-nonce").build();
        final DotRuntimeException ex = assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
        assertTrue(ex.getMessage().contains("nonce"), ex.getMessage());
    }

    @Test
    void nonceMissing_throws() {
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(CLIENT_ID))
                .expirationTime(new Date(System.currentTimeMillis() + 60_000L))
                .subject(SUBJECT)
                .build();
        assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
    }

    @Test
    void expectedNonceBlank_throws() {
        assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(validBuilder().build(), ISSUER, CLIENT_ID, "   "));
    }

    @Test
    void subMissing_throws() {
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(List.of(CLIENT_ID))
                .expirationTime(new Date(System.currentTimeMillis() + 60_000L))
                .claim("nonce", NONCE)
                .build();
        final DotRuntimeException ex = assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
        assertTrue(ex.getMessage().contains("sub"), ex.getMessage());
    }

    @Test
    void audEmptyList_throws() {
        final JWTClaimsSet claims = validBuilder().audience(Collections.emptyList()).build();
        assertThrows(DotRuntimeException.class, () ->
                OIDCProvider.verifyIdTokenClaims(claims, ISSUER, CLIENT_ID, NONCE));
    }
}
