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

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;

public class DSAParameters
    implements CipherParameters
{
    private BigInteger              g;
    private BigInteger              q;
    private BigInteger              p;
    private DSAValidationParameters validation;

    public DSAParameters(
        BigInteger  p,
        BigInteger  q,
        BigInteger  g)
    {
        this.g = g;
        this.p = p;
        this.q = q;
    }   

    public DSAParameters(
        BigInteger              p,
        BigInteger              q,
        BigInteger              g,
        DSAValidationParameters params)
    {
        this.g = g;
        this.p = p;
        this.q = q;
        this.validation = params;
    }   

    public BigInteger getP()
    {
        return p;
    }

    public BigInteger getQ()
    {
        return q;
    }

    public BigInteger getG()
    {
        return g;
    }

    public DSAValidationParameters getValidationParameters()
    {
        return validation;
    }

    public boolean equals(
        Object  obj)
    {
        if (!(obj instanceof DSAParameters))
        {
            return false;
        }

        DSAParameters    pm = (DSAParameters)obj;

        return (pm.getP().equals(p) && pm.getQ().equals(q) && pm.getG().equals(g));
    }
    
    public int hashCode()
    {
        return getP().hashCode() ^ getQ().hashCode() ^ getG().hashCode();
    }
}
