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

public class DHPublicKeyParameters
    extends DHKeyParameters
{
    private BigInteger      y;

    public DHPublicKeyParameters(
        BigInteger      y,
        DHParameters    params)
    {
        super(false, params);

        this.y = y;
    }   

    public BigInteger getY()
    {
        return y;
    }

    public int hashCode()
    {
        return y.hashCode() ^ super.hashCode();
    }

    public boolean equals(
        Object  obj)
    {
        if (!(obj instanceof DHPublicKeyParameters))
        {
            return false;
        }

        DHPublicKeyParameters   other = (DHPublicKeyParameters)obj;

        return other.getY().equals(y) && super.equals(obj);
    }
}
