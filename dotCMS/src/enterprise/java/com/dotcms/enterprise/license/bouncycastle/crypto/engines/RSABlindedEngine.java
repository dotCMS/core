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

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * this does your basic RSA algorithm with blinding
 */
public class RSABlindedEngine
    implements AsymmetricBlockCipher
{
    private static BigInteger ONE = BigInteger.valueOf(1);

    private RSACoreEngine    core = new RSACoreEngine();
    private RSAKeyParameters key;
    private SecureRandom     random;

    /**
     * initialise the RSA engine.
     *
     * @param forEncryption true if we are encrypting, false otherwise.
     * @param param the necessary RSA key parameters.
     */
    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        core.init(forEncryption, param);

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            key = (RSAKeyParameters)rParam.getParameters();
            random = rParam.getRandom();
        }
        else
        {
            key = (RSAKeyParameters)param;
            random = new SecureRandom();
        }
    }

    /**
     * Return the maximum size for an input block to this engine.
     * For RSA this is always one byte less than the key size on
     * encryption, and the same length as the key size on decryption.
     *
     * @return maximum size for an input block.
     */
    public int getInputBlockSize()
    {
        return core.getInputBlockSize();
    }

    /**
     * Return the maximum size for an output block to this engine.
     * For RSA this is always one byte less than the key size on
     * decryption, and the same length as the key size on encryption.
     *
     * @return maximum size for an output block.
     */
    public int getOutputBlockSize()
    {
        return core.getOutputBlockSize();
    }

    /**
     * Process a single block using the basic RSA algorithm.
     *
     * @param in the input array.
     * @param inOff the offset into the input buffer where the data starts.
     * @param inLen the length of the data to be processed.
     * @return the result of the RSA process.
     * @exception DataLengthException the input block is too large.
     */
    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
    {
        if (key == null)
        {
            throw new IllegalStateException("RSA engine not initialised");
        }

        BigInteger input = core.convertInput(in, inOff, inLen);

        BigInteger result;
        if (key instanceof RSAPrivateCrtKeyParameters)
        {
            RSAPrivateCrtKeyParameters k = (RSAPrivateCrtKeyParameters)key;

            BigInteger e = k.getPublicExponent();
            if (e != null)   // can't do blinding without a public exponent
            {
                BigInteger m = k.getModulus();
                BigInteger r = BigIntegers.createRandomInRange(ONE, m.subtract(ONE), random);

                BigInteger blindedInput = r.modPow(e, m).multiply(input).mod(m);
                BigInteger blindedResult = core.processBlock(blindedInput);

                BigInteger rInv = r.modInverse(m);
                result = blindedResult.multiply(rInv).mod(m);
            }
            else
            {
                result = core.processBlock(input);
            }
        }
        else
        {
            result = core.processBlock(input);
        }

        return core.convertOutput(result);
    }
}
