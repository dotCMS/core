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

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Generate a random factor suitable for use with RSA blind signatures
 * as outlined in Chaum's blinding and unblinding as outlined in
 * "Handbook of Applied Cryptography", page 475.
 */
public class RSABlindingFactorGenerator
{
    private static BigInteger ZERO = BigInteger.valueOf(0);
    private static BigInteger ONE = BigInteger.valueOf(1);

    private RSAKeyParameters key;
    private SecureRandom random;

    /**
     * Initialise the factor generator
     *
     * @param param the necessary RSA key parameters.
     */
    public void init(
        CipherParameters param)
    {
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;

            key = (RSAKeyParameters)rParam.getParameters();
            random = rParam.getRandom();
        }
        else
        {
            key = (RSAKeyParameters)param;
            random = new SecureRandom();
        }

        if (key instanceof RSAPrivateCrtKeyParameters)
        {
            throw new IllegalArgumentException("generator requires RSA public key");
        }
    }

    /**
     * Generate a suitable blind factor for the public key the generator was initialised with.
     *
     * @return a random blind factor
     */
    public BigInteger generateBlindingFactor()
    {
        if (key == null)
        {
            throw new IllegalStateException("generator not initialised");
        }

        BigInteger m = key.getModulus();
        int length = m.bitLength() - 1; // must be less than m.bitLength()
        BigInteger factor;
        BigInteger gcd;

        do
        {
            factor = new BigInteger(length, random);
            gcd = factor.gcd(m);
        }
        while (factor.equals(ZERO) || factor.equals(ONE) || !gcd.equals(ONE));

        return factor;
    }
}
