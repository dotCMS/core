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

import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithm;
import com.dotcms.enterprise.de.qaware.heimdall.config.HashAlgorithmConfig;

/**
 * Hashes and verifies passwords.
 */
public interface Password {
    /**
     * Hashes a given cleartext password with the default configuration.
     *
     * @param cleartext The cleartext password.
     * @return Hash of the cleartext password.
     * @throws PasswordException If hashing failed.
     * @deprecated Use hash(char[]) instead.
     */
    @Deprecated
    String hash(String cleartext) throws PasswordException;

    /**
     * Hashes a given cleartext password with the default configuration.
     *
     * @param cleartext The cleartext password.
     * @return Hash of the cleartext password.
     * @throws PasswordException If hashing failed.
     */
    String hash(char[] cleartext) throws PasswordException;

    /**
     * Hashes a given cleartext password with the default configuration.
     *
     * @param cleartext The cleartext password.
     * @return Hash of the cleartext password.
     * @throws PasswordException If hashing failed.
     */
    String hash(SecureCharArray cleartext) throws PasswordException;

    /**
     * Hashes a given cleartext password with the given hash algorithm and the given configuration.
     * <p/>
     * Be careful when using this method. If invoked with the wrong parameters it is possible
     * that weak password hashes are created.
     *
     * @param cleartext     The cleartext password.
     * @param hashAlgorithm The hash algorithm.
     * @param config        The configuration for the hash algorithm.
     * @return Hash of the cleartext password.
     * @throws PasswordException If hashing failed.
     * @deprecated Use hash(char[], HashAlgorithm, HashAlgorithmConfig) instead.
     */
    @Deprecated
    String hash(String cleartext, HashAlgorithm hashAlgorithm, HashAlgorithmConfig config) throws PasswordException;

    /**
     * Hashes a given cleartext password with the given hash algorithm and the given configuration.
     * <p/>
     * Be careful when using this method. If invoked with the wrong parameters it is possible
     * that weak password hashes are created.
     *
     * @param cleartext     The cleartext password.
     * @param hashAlgorithm The hash algorithm.
     * @param config        The configuration for the hash algorithm.
     * @return Hash of the cleartext password.
     * @throws PasswordException If hashing failed.
     */
    String hash(char[] cleartext, HashAlgorithm hashAlgorithm, HashAlgorithmConfig config) throws PasswordException;

    /**
     * Hashes a given cleartext password with the given hash algorithm and the given configuration.
     * <p/>
     * Be careful when using this method. If invoked with the wrong parameters it is possible
     * that weak password hashes are created.
     *
     * @param cleartext     The cleartext password.
     * @param hashAlgorithm The hash algorithm.
     * @param config        The configuration for the hash algorithm.
     * @return Hash of the cleartext password.
     * @throws PasswordException If hashing failed.
     */
    String hash(SecureCharArray cleartext, HashAlgorithm hashAlgorithm, HashAlgorithmConfig config) throws PasswordException;

    /**
     * Verifies that the given cleartext password matches the given hash.
     *
     * @param cleartext The cleartext password.
     * @param hash      The hash.
     * @return True if the given cleartext password matches the given hash, false otherwise.
     * @throws PasswordException If verification failed.
     * @deprecated Use verify(char[], String) instead.
     */
    @Deprecated
    boolean verify(String cleartext, String hash) throws PasswordException;

    /**
     * Verifies that the given cleartext password matches the given hash.
     *
     * @param cleartext The cleartext password.
     * @param hash      The hash.
     * @return True if the given cleartext password matches the given hash, false otherwise.
     * @throws PasswordException If verification failed.
     */
    boolean verify(char[] cleartext, String hash) throws PasswordException;

    /**
     * Verifies that the given cleartext password matches the given hash.
     *
     * @param cleartext The cleartext password.
     * @param hash      The hash.
     * @return True if the given cleartext password matches the given hash, false otherwise.
     * @throws PasswordException If verification failed.
     */
    boolean verify(SecureCharArray cleartext, String hash) throws PasswordException;

    /**
     * Determines if the given hash needs a rehash.
     * <p/>
     * A rehash is necessary if the hash is not secure anymore.
     *
     * @param hash The hash.
     * @return True if a rehash is necessary, false otherwise.
     * @throws PasswordException If rehash checking failed.
     */
    boolean needsRehash(String hash) throws PasswordException;
}
