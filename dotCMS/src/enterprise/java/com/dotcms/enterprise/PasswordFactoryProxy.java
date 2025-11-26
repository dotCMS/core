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
import com.dotmarketing.util.Logger;

public class PasswordFactoryProxy {

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
        }

        return AuthenticationStatus.NOT_AUTHENTICATED;
    }

    /**
     * Verify is hashedPassword needs a stronger hash or is a plain password.
     * 
     * @param hashedPassword
     * @return true if needs rehash otherwise false
     * @throws PasswordException
     */
    public static boolean isUnsecurePasswordHash(final String hashedPassword) {
        Password password = PasswordFactory.create();

        try {
            password.needsRehash(hashedPassword);
        } catch (PasswordException e) {
            return true;
        }

        return false;
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
