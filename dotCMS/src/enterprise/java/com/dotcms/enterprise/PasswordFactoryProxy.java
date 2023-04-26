/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
