/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import com.dotcms.enterprise.de.qaware.heimdall.Password;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordException;
import com.dotcms.enterprise.de.qaware.heimdall.PasswordFactory;
import com.dotcms.enterprise.de.qaware.heimdall.SecureCharArray;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithm;
import com.dotcms.enterprise.de.qaware.heimdall.config.HashAlgorithmConfig;
import com.dotcms.enterprise.de.qaware.heimdall.salt.SecureSaltProvider;
import java.util.regex.Pattern;

public class PasswordFactoryProxy {

    /** Hexadecimal version prefixes reserve structured records from legacy plaintext fallback. */
    private static final Pattern HASH_RECORD_PREFIX = Pattern.compile("^[0-9a-fA-F]+:");

    public static enum AuthenticationStatus {
        AUTHENTICATED, NOT_AUTHENTICATED, NEEDS_REHASH;
    }

    /**
     * Constructor private because we are using static method
     */
    private PasswordFactoryProxy() {
    }

    public static Password passwordFactoryCreate() {
        return PasswordFactory.create();
    }

    public static SecureSaltProvider secureSaltProvider() {
        return new SecureSaltProvider();
    }

    /**
     * Generate a secure hash from a password
     * 
     * @param passwordFromUser
     *            entered by the user
     * @return
     * @throws PasswordException
     */
    public static String generateHash(final String passwordFromUser) throws PasswordException {
        Password password = PasswordFactory.create();

        // Read cleartext password from user
        try (SecureCharArray cleartext = new SecureCharArray(passwordFromUser.toCharArray())) {
            return password.hash(cleartext);
        }
    }

    /**
     * Generate a secure hash from a password
     * 
     * @param passwordFromUser
     *            entered by the user
     * @param hashAlgorithm
     * @param hashAlgorithmConfig
     * @return
     * @throws PasswordException
     */
    public static String generateHash(final String passwordFromUser, HashAlgorithm hashAlgorithm,
            HashAlgorithmConfig hashAlgorithmConfig) throws PasswordException {
        Password password = PasswordFactory.create();

        // Read cleartext password from user
        try (SecureCharArray cleartext = new SecureCharArray(passwordFromUser.toCharArray())) {
            return password.hash(cleartext, hashAlgorithm, hashAlgorithmConfig);
        }
    }

    /**
     * Check if the password entered by the user is equals to the password
     * loaded from the system.
     * 
     * @param passwordFromUser
     *            entered by the user (clear password)
     * @param passwordFromDb
     *            loaded from system
     * @return status (authenticated, not authenticated or needs rehash)
     * @throws PasswordException
     */
    public static AuthenticationStatus authPassword(final String passwordFromUser, final String passwordFromDb)
            throws PasswordException {
        Password password = PasswordFactory.create();

        // Read cleartext password from user
        try (SecureCharArray cleartext = new SecureCharArray(passwordFromUser.toCharArray())) {
            if (password.verify(cleartext, passwordFromDb)) {
                if (password.needsRehash(passwordFromDb)) {
                    // Check if the hash uses an old hash algorithm, insecure
                    // parameters, etc.
                    return AuthenticationStatus.NEEDS_REHASH;
                } else {
                    // Password is correct, proceed...
                    return AuthenticationStatus.AUTHENTICATED;
                }
            }
        } catch (IllegalArgumentException e) {
            throw new PasswordException(e);
        }

        return AuthenticationStatus.NOT_AUTHENTICATED;
    }

    /**
     * Identifies pre-Heimdall plaintext or digest records eligible for legacy migration.
     * Structured records retain that identity even when their version, profile, or fields
     * cannot be verified, so a stored hash can never become a plaintext credential.
     *
     * @param hashedPassword the stored password record
     * @return {@code true} only when the record has no reserved hexadecimal version prefix
     */
    public static boolean isUnsecurePasswordHash(final String hashedPassword) {
        return !HASH_RECORD_PREFIX.matcher(hashedPassword).find();
    }

    /**
     * Utility method that generates salts with a size in bits
     * 
     * @param sizeInBits
     * @return byte array with salt
     */
    public static byte[] generateSalt(final int sizeInBits) {
        SecureSaltProvider sut = new SecureSaltProvider();

        return sut.create(sizeInBits);
    }
}
