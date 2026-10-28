/*
 * Copyright (c) 2026 dotCMS LLC
 * Use of this software is governed by the Business Source License included
 * in the LICENSE file found at the root directory of this software.
 * SPDX-License-Identifier: BUSL-1.1
 */
package com.dotcms.enterprise.de.qaware.heimdall.algorithm;

/**
 * PBKDF2 with a 256-bit output and a distinct identity from the legacy 192-bit profile.
 * Algorithm and iteration settings remain shared with {@link PBKDF2}.
 */
public class PBKDF2_256 extends PBKDF2 {

    /**
     * Identifies 256-bit records without changing the meaning of legacy profile ID 1.
     *
     * @return the serialized profile ID, 2
     */
    @Override
    public int getId() {
        return 2;
    }

    /**
     * Keeps this profile current while inherited configuration checks enforce iteration policy.
     *
     * @return {@code false} because this is the current password profile
     */
    @Override
    public boolean isAlgorithmDeprecated() {
        return false;
    }

    /**
     * Sets the derived output length for new password records.
     *
     * @return 256 bits
     */
    @Override
    public int getOutputSizeInBits() {
        return 256;
    }
}
