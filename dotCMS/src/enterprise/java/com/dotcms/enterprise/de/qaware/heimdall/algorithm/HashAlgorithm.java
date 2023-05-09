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
package com.dotcms.enterprise.de.qaware.heimdall.algorithm;

import com.dotcms.enterprise.de.qaware.heimdall.config.HashAlgorithmConfig;

/**
 * A hash algorithm.
 */
public interface HashAlgorithm {
    /**
     * Id of the hash algorithm. Must be unique.
     *
     * @return Id of the hash algorithm.
     */
    int getId();

    /**
     * Determines if the hash algorithm is deprecated.
     *
     * @return True if the hash algorithm is deprecated, false otherwise.
     */
    boolean isAlgorithmDeprecated();

    /**
     * Determines if the given config is deprecated.
     *
     * @param config The config.
     * @return True if the config is deprecated, false otherwise.
     * @throws AlgorithmException If determining if the config is deprecated failed.
     */
    boolean isConfigDeprecated(HashAlgorithmConfig config) throws AlgorithmException;

    /**
     * Returns the default config.
     *
     * @return The default config.
     */
    HashAlgorithmConfig getDefaultConfig();

    /**
     * Returns the output size in bits.
     *
     * @return Output size in bits.
     */
    int getOutputSizeInBits();

    /**
     * Hashes a given password with the given salt and config.
     *
     * @param password The password to hash.
     * @param salt     The salt.
     * @param config   The config.
     * @return Hashed password.
     * @throws AlgorithmException If hashing the password failed.
     */
    byte[] hash(char[] password, byte[] salt, HashAlgorithmConfig config) throws AlgorithmException;
}
