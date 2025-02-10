/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto;

/**
 * base interface for general purpose byte derivation functions.
 */
public interface DerivationFunction
{
    public void init(DerivationParameters param);

    /**
     * return the message digest used as the basis for the function
     */
    public Digest getDigest();

    public int generateBytes(byte[] out, int outOff, int len)
        throws DataLengthException, IllegalArgumentException;
}
