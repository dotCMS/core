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

import com.dotcms.enterprise.license.bouncycastle.math.ec.ECConstants;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECCurve;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

public class ECDomainParameters
    implements ECConstants
{
    ECCurve     curve;
    byte[]      seed;
    ECPoint     G;
    BigInteger  n;
    BigInteger  h;

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n)
    {
        this.curve = curve;
        this.G = G;
        this.n = n;
        this.h = ONE;
        this.seed = null;
    }

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n,
        BigInteger  h)
    {
        this.curve = curve;
        this.G = G;
        this.n = n;
        this.h = h;
        this.seed = null;
    }

    public ECDomainParameters(
        ECCurve     curve,
        ECPoint     G,
        BigInteger  n,
        BigInteger  h,
        byte[]      seed)
    {
        this.curve = curve;
        this.G = G;
        this.n = n;
        this.h = h;
        this.seed = seed;
    }

    public ECCurve getCurve()
    {
        return curve;
    }

    public ECPoint getG()
    {
        return G;
    }

    public BigInteger getN()
    {
        return n;
    }

    public BigInteger getH()
    {
        return h;
    }

    public byte[] getSeed()
    {
        return seed;
    }
}
