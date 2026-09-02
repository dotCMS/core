package com.dotcms.rest.api.v1.maintenance;

import com.dotmarketing.exception.DotRuntimeException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Shared helpers used by the Maintenance Logged Users views (legacy DWR and REST)
 * to keep raw HTTP session IDs off the wire.
 * <p>
 * Real session IDs are HMAC-SHA256'd with a per-caller CSRF secret, truncated to
 * 16 bytes and Base64-URL encoded (no padding). Clients only ever see this
 * derived token; the server validates a returned token by recomputing the HMAC
 * for each active session.
 *
 * @author hassandotcms
 */
public final class SessionTokenUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int TOKEN_BYTE_LENGTH = 16;

    private SessionTokenUtil() {
    }

    /**
     * Returns the HMAC-SHA256 of the given session id, truncated to 16 bytes and
     * Base64-URL encoded without padding.
     */
    public static String obfuscateSessionId(final String sessionId, final String secretKey) {
        try {
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            final byte[] hash = mac.doFinal(sessionId.getBytes(StandardCharsets.UTF_8));
            final byte[] truncated = new byte[TOKEN_BYTE_LENGTH];
            System.arraycopy(hash, 0, truncated, 0, TOKEN_BYTE_LENGTH);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated);
        } catch (final GeneralSecurityException e) {
            throw new DotRuntimeException("HMAC-SHA256 is not available in this JVM", e);
        }
    }

    /**
     * Returns {@code true} when the supplied {@code token} is the HMAC of
     * {@code sessionId} under {@code secretKey}. Uses a constant-time comparison
     * to avoid leaking match progress through timing.
     */
    public static boolean validateSessionId(final String sessionId, final String secretKey, final String token) {
        if (token == null) {
            return false;
        }
        final byte[] expected = obfuscateSessionId(sessionId, secretKey).getBytes(StandardCharsets.UTF_8);
        final byte[] provided = token.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }
}
