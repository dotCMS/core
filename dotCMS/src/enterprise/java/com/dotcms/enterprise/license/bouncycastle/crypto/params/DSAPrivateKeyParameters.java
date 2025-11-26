/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import java.math.BigInteger;

public class DSAPrivateKeyParameters
    extends DSAKeyParameters
{
    private BigInteger      x;

    public DSAPrivateKeyParameters(
        BigInteger      x,
        DSAParameters   params)
    {
        super(true, params);

        this.x = x;
    }   

    public BigInteger getX()
    {
        return x;
    }
}
