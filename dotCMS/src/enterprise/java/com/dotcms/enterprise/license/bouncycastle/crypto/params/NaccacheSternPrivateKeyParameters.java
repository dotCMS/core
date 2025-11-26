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
import java.util.Vector;

/**
 * Private key parameters for NaccacheStern cipher. For details on this cipher,
 * please see
 * 
 * http://www.gemplus.com/smart/rd/publications/pdf/NS98pkcs.pdf
 */
public class NaccacheSternPrivateKeyParameters extends NaccacheSternKeyParameters 
{
    private BigInteger phi_n;
    private Vector     smallPrimes;

    /**
     * Constructs a NaccacheSternPrivateKey
     * 
     * @param g
     *            the public enryption parameter g
     * @param n
     *            the public modulus n = p*q
     * @param lowerSigmaBound
     *            the public lower sigma bound up to which data can be encrypted
     * @param smallPrimes
     *            the small primes, of which sigma is constructed in the right
     *            order
     * @param phi_n
     *            the private modulus phi(n) = (p-1)(q-1)
     */
    public NaccacheSternPrivateKeyParameters(BigInteger g, BigInteger n,
            int lowerSigmaBound, Vector smallPrimes,
            BigInteger phi_n)
    {
        super(true, g, n, lowerSigmaBound);
        this.smallPrimes = smallPrimes;
        this.phi_n = phi_n;
    }

    public BigInteger getPhi_n()
    {
        return phi_n;
    }

    public Vector getSmallPrimes()
    {
        return smallPrimes;
    }
}
