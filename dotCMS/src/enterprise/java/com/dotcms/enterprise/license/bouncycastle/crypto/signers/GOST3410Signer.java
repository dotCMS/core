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
import com.dotcms.enterprise.license.bouncycastle.crypto.params.*;

import java.security.SecureRandom;
import java.math.BigInteger;

/**
 * GOST R 34.10-94 Signature Algorithm
 */
public class GOST3410Signer
        implements DSA
{
        GOST3410KeyParameters key;

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
                    this.key = (GOST3410PrivateKeyParameters)rParam.getParameters();
                }
                else
                {
                    this.random = new SecureRandom();
                    this.key = (GOST3410PrivateKeyParameters)param;
                }
            }
            else
            {
                this.key = (GOST3410PublicKeyParameters)param;
            }
        }

        /**
         * generate a signature for the given message using the key we were
         * initialised with. For conventional GOST3410 the message should be a GOST3411
         * hash of the message of interest.
         *
         * @param message the message that will be verified later.
         */
        public BigInteger[] generateSignature(
            byte[] message)
        {
            byte[] mRev = new byte[message.length]; // conversion is little-endian
            for (int i = 0; i != mRev.length; i++)
            {
                mRev[i] = message[mRev.length - 1 - i];
            }
            
            BigInteger      m = new BigInteger(1, mRev);
            GOST3410Parameters   params = key.getParameters();
            BigInteger      k;

            do
            {
                k = new BigInteger(params.getQ().bitLength(), random);
            }
            while (k.compareTo(params.getQ()) >= 0);

            BigInteger  r = params.getA().modPow(k, params.getP()).mod(params.getQ());

            BigInteger  s = k.multiply(m).
                                add(((GOST3410PrivateKeyParameters)key).getX().multiply(r)).
                                    mod(params.getQ());

            BigInteger[]  res = new BigInteger[2];

            res[0] = r;
            res[1] = s;

            return res;
        }

        /**
         * return true if the value r and s represent a GOST3410 signature for
         * the passed in message for standard GOST3410 the message should be a
         * GOST3411 hash of the real message to be verified.
         */
        public boolean verifySignature(
            byte[]      message,
            BigInteger  r,
            BigInteger  s)
        {
            byte[] mRev = new byte[message.length]; // conversion is little-endian
            for (int i = 0; i != mRev.length; i++)
            {
                mRev[i] = message[mRev.length - 1 - i];
            }
            
            BigInteger           m = new BigInteger(1, mRev);
            GOST3410Parameters   params = key.getParameters();
            BigInteger           zero = BigInteger.valueOf(0);

            if (zero.compareTo(r) >= 0 || params.getQ().compareTo(r) <= 0)
            {
                return false;
            }

            if (zero.compareTo(s) >= 0 || params.getQ().compareTo(s) <= 0)
            {
                return false;
            }

            BigInteger  v = m.modPow(params.getQ().subtract(new BigInteger("2")),params.getQ());

            BigInteger  z1 = s.multiply(v).mod(params.getQ());
            BigInteger  z2 = (params.getQ().subtract(r)).multiply(v).mod(params.getQ());
            
            z1 = params.getA().modPow(z1, params.getP());
            z2 = ((GOST3410PublicKeyParameters)key).getY().modPow(z2, params.getP());

            BigInteger  u = z1.multiply(z2).mod(params.getP()).mod(params.getQ());

            return u.equals(r);
        }
}
