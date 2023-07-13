package com.dotcms.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for encrypting data using various algorithms.
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.13 $
 */
public class Encryptor {

    public static final String SHA256_ALGORITHM = "SHA-256";

    /**
     * Provides a builder for creating SHA-256 hashes.
     */
    public static class Hashing {

        /**
         * Creates a new HashBuilder for SHA-256 hashing.
         *
         * @return a new HashBuilder instance
         * @throws NoSuchAlgorithmException if the SHA-256 algorithm is not available
         */
        public static HashBuilder sha256() throws NoSuchAlgorithmException {

            return new SHA236HashBuilder();
        }
    }

    private static class SHA236HashBuilder implements HashBuilder {

        private static final char[] HEXADECIMAL_ALPHABET_ARRAY = "0123456789abcdef".toCharArray();
        private final MessageDigest sha256;

        /**
         * Constructs a SHA-256 HashBuilder.
         *
         * @throws NoSuchAlgorithmException if the SHA-256 algorithm is not available
         */
        private SHA236HashBuilder() throws NoSuchAlgorithmException {

            this.sha256 = MessageDigest.getInstance(SHA256_ALGORITHM);
        }

        @Override
        public HashBuilder append(final byte[] bytes) {

            this.sha256.update(bytes);
            return this;
        }

        @Override
        public HashBuilder append(final byte[] bytes, final int maxBytes) {
            this.sha256.update(bytes, 0, maxBytes);
            return this;
        }

        @Override
        public String buildHexa() {
            return Base64.encode(this.buildBytes());
        }

        @Override
        public byte[] buildBytes() {
            return this.sha256.digest();
        }

        @Override
        public String buildUnixHash() {

            final byte[] bytes = this.buildBytes();
            final StringBuilder hashBuilder = new StringBuilder(2 * bytes.length);
            for (final byte _byte : bytes) {

                hashBuilder.append(HEXADECIMAL_ALPHABET_ARRAY[(_byte >> 4) & 0xf])
                        .append(HEXADECIMAL_ALPHABET_ARRAY[_byte & 0xf]);
            }

            return hashBuilder.toString();
        }

    }
}
