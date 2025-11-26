/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.agreement.kdf;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.crypto.DerivationParameters;

public class DHKDFParameters
    implements DerivationParameters
{
    private final DERObjectIdentifier algorithm;
    private final int keySize;
    private final byte[] z;
    private final byte[] extraInfo;

    public DHKDFParameters(
        DERObjectIdentifier algorithm,
        int keySize,
        byte[] z)
    {
        this.algorithm = algorithm;
        this.keySize = keySize;
        this.z = z;
        this.extraInfo = null;
    }

    public DHKDFParameters(
        DERObjectIdentifier algorithm,
        int keySize,
        byte[] z,
        byte[] extraInfo)
    {
        this.algorithm = algorithm;
        this.keySize = keySize;
        this.z = z;
        this.extraInfo = extraInfo;
    }

    public DERObjectIdentifier getAlgorithm()
    {
        return algorithm;
    }

    public int getKeySize()
    {
        return keySize;
    }

    public byte[] getZ()
    {
        return z;
    }

    public byte[] getExtraInfo()
    {
        return extraInfo;
    }
}
