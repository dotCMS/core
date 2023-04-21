/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

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
