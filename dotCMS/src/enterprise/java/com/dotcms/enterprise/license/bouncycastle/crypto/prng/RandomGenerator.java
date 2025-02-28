/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.prng;

/**
 * Generic interface for objects generating random bytes.
 */
public interface RandomGenerator
{
    /**
     * Add more seed material to the generator.
     *
     * @param seed a byte array to be mixed into the generator's state.
     */
    void addSeedMaterial(byte[] seed);

    /**
     * Add more seed material to the generator.
     *
     * @param seed a long value to be mixed into the generator's state.
     */
    void addSeedMaterial(long seed);

    /**
     * Fill bytes with random values.
     *
     * @param bytes byte array to be filled.
     */
    void nextBytes(byte[] bytes);

    /**
     * Fill part of bytes with random values.
     *
     * @param bytes byte array to be filled.
     * @param start index to start filling at.
     * @param len length of segment to fill.
     */
    void nextBytes(byte[] bytes, int start, int len);

}
