/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;

/**
 * parameters for using an integrated cipher in stream mode.
 */
public class IESParameters
    implements CipherParameters
{
    private byte[]  derivation;
    private byte[]  encoding;
    private int     macKeySize;

    /**
     * @param derivation the derivation parameter for the KDF function.
     * @param encoding the encoding parameter for the KDF function.
     * @param macKeySize the size of the MAC key (in bits).
     */
    public IESParameters(
        byte[]  derivation,
        byte[]  encoding,
        int     macKeySize)
    {
        this.derivation = derivation;
        this.encoding = encoding;
        this.macKeySize = macKeySize;
    }

    public byte[] getDerivationV()
    {
        return derivation;
    }

    public byte[] getEncodingV()
    {
        return encoding;
    }

    public int getMacKeySize()
    {
        return macKeySize;
    }
}
