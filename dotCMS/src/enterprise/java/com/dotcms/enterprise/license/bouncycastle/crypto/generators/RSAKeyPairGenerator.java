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

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.KeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import java.math.BigInteger;

/**
 * an RSA key pair generator.
 */
public class RSAKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private static final BigInteger ONE = BigInteger.valueOf(1);

    private RSAKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (RSAKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        BigInteger    p, q, n, d, e, pSub1, qSub1, phi;

        //
        // p and q values should have a length of half the strength in bits
        //
        int strength = param.getStrength();
        int pbitlength = (strength + 1) / 2;
        int qbitlength = strength - pbitlength;
        int mindiffbits = strength / 3;

        e = param.getPublicExponent();

        // TODO Consider generating safe primes for p, q (see DHParametersHelper.generateSafePrimes)
        // (then p-1 and q-1 will not consist of only small factors - see "Pollard's algorithm")

        //
        // generate p, prime and (p-1) relatively prime to e
        //
        for (;;)
        {
            p = new BigInteger(pbitlength, 1, param.getRandom());
            
            if (p.mod(e).equals(ONE))
            {
                continue;
            }
            
            if (!p.isProbablePrime(param.getCertainty()))
            {
                continue;
            }
            
            if (e.gcd(p.subtract(ONE)).equals(ONE)) 
            {
                break;
            }
        }

        //
        // generate a modulus of the required length
        //
        for (;;)
        {
            // generate q, prime and (q-1) relatively prime to e,
            // and not equal to p
            //
            for (;;)
            {
                q = new BigInteger(qbitlength, 1, param.getRandom());

                if (q.subtract(p).abs().bitLength() < mindiffbits)
                {
                    continue;
                }
                
                if (q.mod(e).equals(ONE))
                {
                    continue;
                }
            
                if (!q.isProbablePrime(param.getCertainty()))
                {
                    continue;
                }
            
                if (e.gcd(q.subtract(ONE)).equals(ONE)) 
                {
                    break;
                } 
            }

            //
            // calculate the modulus
            //
            n = p.multiply(q);

            if (n.bitLength() == param.getStrength()) 
            {
                break;
            } 

            //
            // if we get here our primes aren't big enough, make the largest
            // of the two p and try again
            //
            p = p.max(q);
        }

        if (p.compareTo(q) < 0)
        {
            phi = p;
            p = q;
            q = phi;
        }

        pSub1 = p.subtract(ONE);
        qSub1 = q.subtract(ONE);
        phi = pSub1.multiply(qSub1);

        //
        // calculate the private exponent
        //
        d = e.modInverse(phi);

        //
        // calculate the CRT factors
        //
        BigInteger    dP, dQ, qInv;

        dP = d.remainder(pSub1);
        dQ = d.remainder(qSub1);
        qInv = q.modInverse(p);

        return new AsymmetricCipherKeyPair(
                new RSAKeyParameters(false, n, e),
                new RSAPrivateCrtKeyParameters(n, e, d, p, q, dP, dQ, qInv));
    }
}
