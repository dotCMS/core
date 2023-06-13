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
import com.dotcms.enterprise.de.qaware.heimdall.util.Preconditions;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Implementation of the PBKDF2 algorithm.
 */
public class PBKDF2 implements HashAlgorithm {
    /**
     * Output size of the hash function in bit.
     */
    private static final int OUTPUT_SIZE_IN_BITS = 192;

    /**
     * Config key to store the iterations.
     */
    public static final String ITERATIONS_CONFIG_KEY = "i";

    /**
     * Name of the PBKDF2 algorithm in the Java Security library.
     */
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Default number of iterations.
     */
    private static final int DEFAULT_ITERATIONS = 20000;

    /**
     * Minimum number of iterations.
     */
    private static final int MINIMUM_ITERATIONS = 10000;

    /**
     * ID of the algorithm.
     */
    private static final int ID = 1;

    /**
     * Radix for hex numbers.
     */
    private static final int RADIX_HEX = 16;

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public boolean isAlgorithmDeprecated() {
        return false;
    }

    @Override
    public boolean isConfigDeprecated(HashAlgorithmConfig config) throws AlgorithmException {
        Preconditions.checkNotNull(config, "config");

        int iterations = getIterationsFromConfig(config);

        return iterations < MINIMUM_ITERATIONS;
    }

    @Override
    public HashAlgorithmConfig getDefaultConfig() {
        HashAlgorithmConfig defaultConfig = new HashAlgorithmConfig();
        defaultConfig.put(ITERATIONS_CONFIG_KEY, Integer.toHexString(DEFAULT_ITERATIONS));

        return defaultConfig;
    }

    @Override
    public int getOutputSizeInBits() {
        return OUTPUT_SIZE_IN_BITS;
    }

    @Override
    public byte[] hash(char[] password, byte[] salt, HashAlgorithmConfig config) throws AlgorithmException {
        Preconditions.checkNotNull(password, "password");
        Preconditions.checkNotNull(salt, "salt");
        Preconditions.checkNotNull(config, "config");

        int iterations = getIterationsFromConfig(config);

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, OUTPUT_SIZE_IN_BITS);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new AlgorithmException(e);
        } catch (InvalidKeySpecException e) {
            throw new AlgorithmException(e);
        }
    }

    /**
     * Reads the iteration count from the given config.
     *
     * @param config Config.
     * @return Iteration count.
     */
    private int getIterationsFromConfig(HashAlgorithmConfig config) throws AlgorithmException {
        assert config != null;

        String value = config.get(ITERATIONS_CONFIG_KEY);
        if (value == null) {
            throw new AlgorithmException("Iteration count config value '" + ITERATIONS_CONFIG_KEY + "' doesn't exist");
        }

        return Integer.parseInt(value, RADIX_HEX);
    }
}
