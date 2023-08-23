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

package com.dotcms.enterprise.license.bouncycastle.crypto.signers;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DSA;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * The Digital Signature Algorithm - as described in "Handbook of Applied
 * Cryptography", pages 452 - 453.
 */
public class DSASigner
    implements DSA
{
    DSAKeyParameters key;

    SecureRandom    random;

    public void init(
        boolean                 forSigning,
        CipherParameters        param)
    {
        if (forSigning)
        {
            if (param instanceof ParametersWithRandom)
            {
                ParametersWithRandom    rParam = (ParametersWithRandom)param;

                this.random = rParam.getRandom();
                this.key = (DSAPrivateKeyParameters)rParam.getParameters();
            }
            else
            {
                this.random = new SecureRandom();
                this.key = (DSAPrivateKeyParameters)param;
            }
        }
        else
        {
            this.key = (DSAPublicKeyParameters)param;
        }
    }

    /**
     * generate a signature for the given message using the key we were
     * initialised with. For conventional DSA the message should be a SHA-1
     * hash of the message of interest.
     *
     * @param message the message that will be verified later.
     */
    public BigInteger[] generateSignature(
        byte[] message)
    {
        DSAParameters   params = key.getParameters();
        BigInteger      m = calculateE(params.getQ(), message);
        BigInteger      k;
        int                  qBitLength = params.getQ().bitLength();

        do 
        {
            k = new BigInteger(qBitLength, random);
        }
        while (k.compareTo(params.getQ()) >= 0);

        BigInteger  r = params.getG().modPow(k, params.getP()).mod(params.getQ());

        k = k.modInverse(params.getQ()).multiply(
                    m.add(((DSAPrivateKeyParameters)key).getX().multiply(r)));

        BigInteger  s = k.mod(params.getQ());

        BigInteger[]  res = new BigInteger[2];

        res[0] = r;
        res[1] = s;

        return res;
    }

    /**
     * return true if the value r and s represent a DSA signature for
     * the passed in message for standard DSA the message should be a
     * SHA-1 hash of the real message to be verified.
     */
    public boolean verifySignature(
        byte[]      message,
        BigInteger  r,
        BigInteger  s)
    {
        DSAParameters   params = key.getParameters();
        BigInteger      m = calculateE(params.getQ(), message);
        BigInteger      zero = BigInteger.valueOf(0);

        if (zero.compareTo(r) >= 0 || params.getQ().compareTo(r) <= 0)
        {
            return false;
        }

        if (zero.compareTo(s) >= 0 || params.getQ().compareTo(s) <= 0)
        {
            return false;
        }

        BigInteger  w = s.modInverse(params.getQ());

        BigInteger  u1 = m.multiply(w).mod(params.getQ());
        BigInteger  u2 = r.multiply(w).mod(params.getQ());

        u1 = params.getG().modPow(u1, params.getP());
        u2 = ((DSAPublicKeyParameters)key).getY().modPow(u2, params.getP());

        BigInteger  v = u1.multiply(u2).mod(params.getP()).mod(params.getQ());

        return v.equals(r);
    }

    private BigInteger calculateE(BigInteger n, byte[] message)
    {
        if (n.bitLength() >= message.length * 8)
        {
            return new BigInteger(1, message);
        }
        else
        {
            byte[] trunc = new byte[n.bitLength() / 8];

            System.arraycopy(message, 0, trunc, 0, trunc.length);

            return new BigInteger(1, trunc);
        }
    }
}
