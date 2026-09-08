package com.dotcms.auth.providers.oauth.provider;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Crypto helpers shared across OAuth providers: HTTP Basic auth header construction
 * that keeps the client secret in a {@code char[]} for as long as possible, and
 * PKCE (RFC 7636) verifier/challenge generation.
 */
public final class OAuthCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OAuthCrypto() {}

    /**
     * Build an {@code Authorization: Basic} header value from a client id and char[] secret.
     * The intermediate byte buffer is zeroed after encoding so the secret spends minimal time
     * as a heap object the GC cannot reclaim.
     */
    public static String basicAuthHeader(final String clientId, final char[] clientSecret) {
        if (!UtilMethods.isSet(clientId) || clientSecret == null) {
            throw new DotRuntimeException("client_id and client_secret are required to build Basic auth");
        }
        final CharBuffer charBuffer = CharBuffer.allocate(
                clientId.length() + 1 + clientSecret.length);
        charBuffer.put(clientId).put(':').put(clientSecret);
        charBuffer.flip();
        final ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        final byte[] encoded = new byte[byteBuffer.remaining()];
        byteBuffer.get(encoded);
        try {
            final String header = "Basic " + Base64.getEncoder().encodeToString(encoded);
            return header;
        } finally {
            Arrays.fill(encoded, (byte) 0);
            if (byteBuffer.hasArray()) {
                Arrays.fill(byteBuffer.array(), (byte) 0);
            }
            Arrays.fill(charBuffer.array(), '\0');
        }
    }

    /**
     * Generate an RFC 7636 PKCE {@code code_verifier}. 64 random bytes encoded as
     * base64url (unpadded) — well within the 43..128 char range the spec requires.
     */
    public static String newPkceVerifier() {
        final byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Derive the S256 code_challenge for a PKCE verifier:
     * {@code base64url(sha256(verifier))}.
     */
    public static String pkceChallengeS256(final String verifier) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            final byte[] digest = sha256.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (final NoSuchAlgorithmException e) {
            // SHA-256 is mandatory on every JVM — any failure here is a platform bug.
            throw new DotRuntimeException("SHA-256 unavailable for PKCE challenge derivation", e);
        }
    }

    /** Generate a random OAuth {@code state} value. */
    public static String newState() {
        return UUID.randomUUID().toString();
    }

    /** Generate a random OIDC {@code nonce} value. */
    public static String newNonce() {
        return UUID.randomUUID().toString();
    }
}
