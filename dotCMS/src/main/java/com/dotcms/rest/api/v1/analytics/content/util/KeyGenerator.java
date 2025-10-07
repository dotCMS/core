package com.dotcms.rest.api.v1.analytics.content.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating random keys. This is used for generating secure authentication keys
 * for Sites in the Content Analytics app. This implementation uses the {@link SecureRandom} class,
 * which is Java’s cryptographically strong random number generator (CSPRNG) that provides enough
 * randomness for attackers to be unable to predict the generated key.
 *
 * @author Jose Castro
 * @since Jul 2nd, 2025
 */
public class KeyGenerator {

    // Number of bytes to generate to get at least 25 Base64 characters, which roughly translates
    // to ~150 bits of entropy: 18 bytes ≈ 24 Base64 characters (no padding)
    private static final int RAW_BYTES_LENGTH = 18;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a secure authentication key that can be used to authentication purposes.
     *
     * @return The generated key.
     */
    public static String generateSiteKey() {
        final byte[] randomBytes = new byte[RAW_BYTES_LENGTH];
        RANDOM.nextBytes(randomBytes);
        // Use URL-safe Base64 encoding and strip padding
        final String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        // Trim or pad to exactly 25 characters if needed
        return encoded.length() >= 25
                ? encoded.substring(0, 25)
                : String.format("%-25s", encoded).replace(' ', 'X'); // pad with 'X' if necessary
    }

}
