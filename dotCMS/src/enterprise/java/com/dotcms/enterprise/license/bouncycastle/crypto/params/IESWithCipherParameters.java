/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;


public class IESWithCipherParameters
    extends IESParameters
{
    private int cipherKeySize;

    /**
     * @param derivation the derivation parameter for the KDF function.
     * @param encoding the encoding parameter for the KDF function.
     * @param macKeySize the size of the MAC key (in bits).
     * @param cipherKeySize the size of the associated Cipher key (in bits).
     */
    public IESWithCipherParameters(
        byte[]  derivation,
        byte[]  encoding,
        int     macKeySize,
        int     cipherKeySize)
    {
        super(derivation, encoding, macKeySize);

        this.cipherKeySize = cipherKeySize;
    }

    public int getCipherKeySize()
    {
        return cipherKeySize;
    }
}
