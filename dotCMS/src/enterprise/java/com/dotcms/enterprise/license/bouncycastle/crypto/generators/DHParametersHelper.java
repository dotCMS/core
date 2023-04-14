package com.dotcms.enterprise.license.bouncycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.util.BigIntegers;

class DHParametersHelper
{
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    // Finds a pair of prime BigInteger's {p, q: p = 2q + 1}
    static BigInteger[] generateSafePrimes(
        int             size,
        int             certainty,
        SecureRandom    random)
    {
        BigInteger p, q;
        int qLength = size - 1;

        for (;;)
        {
            q = new BigInteger(qLength, 2, random);

            // p <- 2q + 1
            p = q.shiftLeft(1).add(ONE);

            if (p.isProbablePrime(certainty)
                && (certainty <= 2 || q.isProbablePrime(certainty)))
            {
                    break;
            }
        }

        return new BigInteger[] { p, q };
    }

    // Select a high order element of the multiplicative group Zp*
    // p and q must be s.t. p = 2*q + 1, where p and q are prime
    static BigInteger selectGenerator(
        BigInteger      p,
        BigInteger      q,
        SecureRandom    random)
    {
        BigInteger pMinusTwo = p.subtract(TWO);
        BigInteger g;

        // Handbook of Applied Cryptography 4.86
        do
        {
            g = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);
        }
        while (g.modPow(TWO, p).equals(ONE)
            || g.modPow(q, p).equals(ONE));

/*
        // RFC 2631 2.1.1 (and see Handbook of Applied Cryptography 4.81)
        do
        {
            BigInteger h = createInRange(TWO, pMinusTwo, random);

            g = h.modPow(TWO, p);
        }
        while (g.equals(ONE));
*/

        return g;
    }
}
