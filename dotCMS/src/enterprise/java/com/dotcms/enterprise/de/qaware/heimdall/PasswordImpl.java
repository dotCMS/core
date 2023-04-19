/*
* The MIT License (MIT)
*
* Copyright (c) 2015 QAware GmbH
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package com.dotcms.enterprise.de.qaware.heimdall;

import com.dotcms.enterprise.de.qaware.heimdall.algorithm.AlgorithmException;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithm;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithmRegistry;
import com.dotcms.enterprise.de.qaware.heimdall.config.ConfigCoder;
import com.dotcms.enterprise.de.qaware.heimdall.config.HashAlgorithmConfig;
import com.dotcms.enterprise.de.qaware.heimdall.salt.SaltProvider;
import com.dotcms.enterprise.de.qaware.heimdall.util.Base64;
import com.dotcms.enterprise.de.qaware.heimdall.util.Preconditions;

import java.util.Arrays;

/**
 * Default implementation for {@link Password}.
 */
public class PasswordImpl implements Password {
    /**
     * Radix for hex.
     */
    private static final int RADIX_HEX = 16;
    /**
     * Delimiter for the parts of the hash.
     */
    private static final String HASH_DELIMITER = ":";

    /**
     * Hash version 1.
     */
    private static final int VERSION_1 = 1;

    /**
     * Current hash version.
     */
    private static final int CURRENT_VERSION = VERSION_1;

    private static final String CLEARTEXT_PARAM = "cleartext";

    /**
     * The salt provider.
     */
    private final SaltProvider saltProvider;
    /**
     * The decoder/encoder for configs.
     */
    private final ConfigCoder configCoder;
    /**
     * Default hash algorithm.
     */
    private final HashAlgorithm defaultHashAlgorithm;

    /**
     * Hash alhgorithm registry.
     */
    private final HashAlgorithmRegistry hashAlgorithmRegistry;

    /**
     * Constructor.
     *
     * @param saltProvider          The salt provider.
     * @param configCoder           The config coder.
     * @param hashAlgorithmRegistry The hash algorithm registry.
     */
    public PasswordImpl(SaltProvider saltProvider, ConfigCoder configCoder, HashAlgorithmRegistry hashAlgorithmRegistry, HashAlgorithm defaultHashAlgorithm) {
        this.saltProvider = Preconditions.checkNotNull(saltProvider, "saltProvider");
        this.configCoder = Preconditions.checkNotNull(configCoder, "configCoder");
        this.hashAlgorithmRegistry = Preconditions.checkNotNull(hashAlgorithmRegistry, "hashAlgorithmRegistry");
        this.defaultHashAlgorithm = Preconditions.checkNotNull(defaultHashAlgorithm, "defaultHashAlgorithm");
    }

    @Override
    public String hash(String cleartext) throws PasswordException {
        Preconditions.checkNotNull(cleartext, CLEARTEXT_PARAM);

        char[] cleartextAsChars = cleartext.toCharArray();
        try {
            return hash(cleartextAsChars);
        } finally {
            purgeArray(cleartextAsChars);
        }
    }

    @Override
    public String hash(SecureCharArray cleartext) throws PasswordException {
        return hash(cleartext.getChars());
    }

    @Override
    public String hash(char[] cleartext) throws PasswordException {
        Preconditions.checkNotNull(cleartext, CLEARTEXT_PARAM);

        return hash(cleartext, defaultHashAlgorithm, defaultHashAlgorithm.getDefaultConfig());
    }

    @Override
    public String hash(String cleartext, HashAlgorithm hashAlgorithm, HashAlgorithmConfig config) throws PasswordException {
        Preconditions.checkNotNull(cleartext, CLEARTEXT_PARAM);
        Preconditions.checkNotNull(hashAlgorithm, "hashAlgorithm");
        Preconditions.checkNotNull(config, "config");

        char[] cleartextAsChars = cleartext.toCharArray();
        try {
            return hash(cleartextAsChars, hashAlgorithm, config);
        } finally {
            purgeArray(cleartextAsChars);
        }
    }

    @Override
    public String hash(SecureCharArray cleartext, HashAlgorithm hashAlgorithm, HashAlgorithmConfig config) throws PasswordException {
        return hash(cleartext.getChars(), hashAlgorithm, config);
    }

    @Override
    public String hash(char[] cleartext, HashAlgorithm hashAlgorithm, HashAlgorithmConfig config) throws PasswordException {
        Preconditions.checkNotNull(cleartext, CLEARTEXT_PARAM);
        Preconditions.checkNotNull(hashAlgorithm, "hashAlgorithm");
        Preconditions.checkNotNull(config, "config");

        int saltSizeInBits = hashAlgorithm.getOutputSizeInBits();

        int id = hashAlgorithm.getId();
        byte[] salt = saltProvider.create(saltSizeInBits);
        byte[] hash;
        try {
            hash = hashAlgorithm.hash(cleartext, salt, config);
        } catch (AlgorithmException e) {
            throw new PasswordException(e);
        }

        return concatenate(id, salt, config, hash);
    }

    @Override
    public boolean verify(SecureCharArray cleartext, String hash) throws PasswordException {
        return verify(cleartext.getChars(), hash);
    }

    @Override
    public boolean verify(String cleartext, String hash) throws PasswordException {
        Preconditions.checkNotNull(cleartext, CLEARTEXT_PARAM);
        Preconditions.checkNotNull(hash, "hash");

        char[] cleartextAsChars = cleartext.toCharArray();
        try {
            return verify(cleartextAsChars, hash);
        } finally {
            purgeArray(cleartextAsChars);
        }
    }

    @Override
    public boolean verify(char[] cleartext, String hash) throws PasswordException {
        Preconditions.checkNotNull(cleartext, CLEARTEXT_PARAM);
        Preconditions.checkNotNull(hash, "hash");

        String[] parts = hash.split(HASH_DELIMITER);

        int version = Integer.parseInt(parts[0], RADIX_HEX);
        switch (version) {
            case VERSION_1:
                return verifyVersion1(cleartext, hash);
            default:
                throw new PasswordException("Unsupported hash version: " + version);
        }
    }

    /**
     * Verifies that the cleartext matches the hash generated by the version 1 of the software.
     *
     * @param cleartext The cleartext.
     * @param hash      The hash.
     * @return True if the given cleartext password matches the given hash, false otherwise.
     * @throws PasswordException If verifying the hash failed.
     */
    private boolean verifyVersion1(char[] cleartext, String hash) throws PasswordException {
        String[] parts = split(hash);

        int id = Integer.parseInt(parts[1], RADIX_HEX);
        byte[] salt = Base64.decode(parts[2]);
        HashAlgorithmConfig config = configCoder.decode(parts[3]);
        byte[] hashBytes = Base64.decode(parts[4]);

        HashAlgorithm hashAlgorithm;
        try {
            hashAlgorithm = hashAlgorithmRegistry.getAlgorithm(id);
        } catch (AlgorithmException e) {
            throw new PasswordException(e);
        }
        byte[] hashedCleartext;
        try {
            hashedCleartext = hashAlgorithm.hash(cleartext, salt, config);
        } catch (AlgorithmException e) {
            throw new PasswordException(e);
        }

        return slowEquals(hashedCleartext, hashBytes);
    }

    @Override
    public boolean needsRehash(String hash) throws PasswordException {
        Preconditions.checkNotNull(hash, "hash");

        String[] parts = split(hash);

        int version = Integer.parseInt(parts[0], RADIX_HEX);
        if (CURRENT_VERSION > version) {
            return true;
        }

        int id = Integer.parseInt(parts[1], RADIX_HEX);
        HashAlgorithmConfig config = configCoder.decode(parts[3]);

        HashAlgorithm algorithm;
        try {
            algorithm = hashAlgorithmRegistry.getAlgorithm(id);
        } catch (AlgorithmException e) {
            throw new PasswordException(e);
        }

        // If algorithm or config is deprecated, a rehash is needed
        try {
            return algorithm.isAlgorithmDeprecated() || algorithm.isConfigDeprecated(config);
        } catch (AlgorithmException e) {
            throw new PasswordException(e);
        }
    }

    /**
     * Purges the content of the given char array.
     *
     * @param array Char array to purge.
     */
    private void purgeArray(char[] array) {
        Arrays.fill(array, '0');
    }

    /**
     * Concatenates the hash parts.
     *
     * @param id     Id of the hash algorithm.
     * @param salt   Salt.
     * @param config Hash algorithm config.
     * @param hash   Hash.
     * @return Concatenated hash parts.
     */
    private String concatenate(int id, byte[] salt, HashAlgorithmConfig config, byte[] hash) {
        assert salt != null;
        assert config != null;
        assert hash != null;

        String versionAsString = Integer.toHexString(CURRENT_VERSION);
        String idAsString = Integer.toHexString(id);
        String saltAsString = Base64.encode(salt);
        String configAsString = configCoder.encode(config);
        String hashAsString = Base64.encode(hash);

        return versionAsString + HASH_DELIMITER + idAsString + HASH_DELIMITER + saltAsString + HASH_DELIMITER + configAsString + HASH_DELIMITER + hashAsString;
    }

    /**
     * Splits a hash in parts.
     *
     * @param hash Hash to split.
     * @return Hash parts.
     * @throws PasswordException If there is a incorrect number of parts.
     */
    private String[] split(String hash) throws PasswordException {
        assert hash != null;

        String[] parts = hash.split(HASH_DELIMITER);
        if (parts.length != 5) {
            throw new PasswordException("Expected a length of 5, but got " + parts.length);
        }
        return parts;
    }

    /**
     * Compares two byte arrays in a way which isn't subject to timing attacks.
     *
     * @param hash1 Hash 1.
     * @param hash2 Hash 2.
     * @return True if both hashes are equal, false otherwise.
     */
    private static boolean slowEquals(byte[] hash1, byte[] hash2) {
        assert hash1 != null;
        assert hash2 != null;

        int diff = hash1.length ^ hash2.length;
        for (int i = 0; i < hash1.length && i < hash2.length; i++) {
            diff |= hash1[i] ^ hash2[i];
        }
        return diff == 0;
    }
}
