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
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * this does your basic ElGamal algorithm.
 */
public class ElGamalEngine
    implements AsymmetricBlockCipher
{
    private ElGamalKeyParameters    key;
    private SecureRandom            random;
    private boolean                 forEncryption;
    private int                     bitSize;

    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    /**
     * initialise the ElGamal engine.
     *
     * @param forEncryption true if we are encrypting, false otherwise.
     * @param param the necessary ElGamal key parameters.
     */
    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    p = (ParametersWithRandom)param;

            this.key = (ElGamalKeyParameters)p.getParameters();
            this.random = p.getRandom();
        }
        else
        {
            this.key = (ElGamalKeyParameters)param;
            this.random = new SecureRandom();
        }

        this.forEncryption = forEncryption;

        BigInteger p = key.getParameters().getP();

        bitSize = p.bitLength();

        if (forEncryption)
        {
            if (!(key instanceof ElGamalPublicKeyParameters))
            {
                throw new IllegalArgumentException("ElGamalPublicKeyParameters are required for encryption.");
            }
        }
        else
        {
            if (!(key instanceof ElGamalPrivateKeyParameters))
            {
                throw new IllegalArgumentException("ElGamalPrivateKeyParameters are required for decryption.");
            }
        }
    }

    /**
     * Return the maximum size for an input block to this engine.
     * For ElGamal this is always one byte less than the size of P on
     * encryption, and twice the length as the size of P on decryption.
     *
     * @return maximum size for an input block.
     */
    public int getInputBlockSize()
    {
        if (forEncryption)
        {
            return (bitSize - 1) / 8;
        }

        return 2 * ((bitSize + 7) / 8);
    }

    /**
     * Return the maximum size for an output block to this engine.
     * For ElGamal this is always one byte less than the size of P on
     * decryption, and twice the length as the size of P on encryption.
     *
     * @return maximum size for an output block.
     */
    public int getOutputBlockSize()
    {
        if (forEncryption)
        {
            return 2 * ((bitSize + 7) / 8);
        }

        return (bitSize - 1) / 8;
    }

    /**
     * Process a single block using the basic ElGamal algorithm.
     *
     * @param in the input array.
     * @param inOff the offset into the input buffer where the data starts.
     * @param inLen the length of the data to be processed.
     * @return the result of the ElGamal process.
     * @exception DataLengthException the input block is too large.
     */
    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
    {
        if (key == null)
        {
            throw new IllegalStateException("ElGamal engine not initialised");
        }

        int maxLength = forEncryption
            ?   (bitSize - 1 + 7) / 8
            :   getInputBlockSize();

        if (inLen > maxLength)
        {
            throw new DataLengthException("input too large for ElGamal cipher.\n");
        }

        BigInteger  p = key.getParameters().getP();

        if (key instanceof ElGamalPrivateKeyParameters) // decryption
        {
            byte[]  in1 = new byte[inLen / 2];
            byte[]  in2 = new byte[inLen / 2];

            System.arraycopy(in, inOff, in1, 0, in1.length);
            System.arraycopy(in, inOff + in1.length, in2, 0, in2.length);

            BigInteger  gamma = new BigInteger(1, in1);
            BigInteger  phi = new BigInteger(1, in2);

            ElGamalPrivateKeyParameters  priv = (ElGamalPrivateKeyParameters)key;
            // a shortcut, which generally relies on p being prime amongst other things.
            // if a problem with this shows up, check the p and g values!
            BigInteger  m = gamma.modPow(p.subtract(ONE).subtract(priv.getX()), p).multiply(phi).mod(p);

            return BigIntegers.asUnsignedByteArray(m);
        }
        else // encryption
        {
            byte[] block;
            if (inOff != 0 || inLen != in.length)
            {
                block = new byte[inLen];

                System.arraycopy(in, inOff, block, 0, inLen);
            }
            else
            {
                block = in;
            }

            BigInteger input = new BigInteger(1, block);

            if (input.bitLength() >= p.bitLength())
            {
                throw new DataLengthException("input too large for ElGamal cipher.\n");
            }

            ElGamalPublicKeyParameters  pub = (ElGamalPublicKeyParameters)key;

            int                         pBitLength = p.bitLength();
            BigInteger                  k = new BigInteger(pBitLength, random);

            while (k.equals(ZERO) || (k.compareTo(p.subtract(TWO)) > 0))
            {
                k = new BigInteger(pBitLength, random);
            }

            BigInteger  g = key.getParameters().getG();
            BigInteger  gamma = g.modPow(k, p);
            BigInteger  phi = input.multiply(pub.getY().modPow(k, p)).mod(p);

            byte[]  out1 = gamma.toByteArray();
            byte[]  out2 = phi.toByteArray();
            byte[]  output = new byte[this.getOutputBlockSize()];

            if (out1.length > output.length / 2)
            {
                System.arraycopy(out1, 1, output, output.length / 2 - (out1.length - 1), out1.length - 1);
            }
            else
            {
                System.arraycopy(out1, 0, output, output.length / 2 - out1.length, out1.length);
            }

            if (out2.length > output.length / 2)
            {
                System.arraycopy(out2, 1, output, output.length - (out2.length - 1), out2.length - 1);
            }
            else
            {
                System.arraycopy(out2, 0, output, output.length - out2.length, out2.length);
            }

            return output;
        }
    }
}
