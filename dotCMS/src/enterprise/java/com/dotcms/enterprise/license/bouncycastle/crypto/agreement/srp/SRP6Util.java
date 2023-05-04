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

package com.dotcms.enterprise.license.bouncycastle.crypto.agreement.srp;

import java.math.BigInteger;
import java.security.SecureRandom;

import com.dotcms.enterprise.license.bouncycastle.crypto.CryptoException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.util.BigIntegers;

public class SRP6Util
{
    private static BigInteger ZERO = BigInteger.valueOf(0);
    private static BigInteger ONE = BigInteger.valueOf(1);

    public static BigInteger calculateK(Digest digest, BigInteger N, BigInteger g)
    {
        return hashPaddedPair(digest, N, N, g);
    }

    public static BigInteger calculateU(Digest digest, BigInteger N, BigInteger A, BigInteger B)
    {
        return hashPaddedPair(digest, N, A, B);
    }

    public static BigInteger calculateX(Digest digest, BigInteger N, byte[] salt, byte[] identity, byte[] password)
    {
        byte[] output = new byte[digest.getDigestSize()];

        digest.update(identity, 0, identity.length);
        digest.update((byte)':');
        digest.update(password, 0, password.length);
        digest.doFinal(output, 0);

        digest.update(salt, 0, salt.length);
        digest.update(output, 0, output.length);
        digest.doFinal(output, 0);

        return new BigInteger(1, output).mod(N);
    }

    public static BigInteger generatePrivateValue(Digest digest, BigInteger N, BigInteger g, SecureRandom random)
    {
        int minBits = Math.min(256, N.bitLength() / 2);
        BigInteger min = ONE.shiftLeft(minBits - 1);
        BigInteger max = N.subtract(ONE);

        return BigIntegers.createRandomInRange(min, max, random);
    }

    public static BigInteger validatePublicValue(BigInteger N, BigInteger val)
        throws CryptoException
    {
        val = val.mod(N);

        // Check that val % N != 0
        if (val.equals(ZERO))
        {
            throw new CryptoException("Invalid public value: 0");
        }

        return val;
    }

    private static BigInteger hashPaddedPair(Digest digest, BigInteger N, BigInteger n1, BigInteger n2)
    {
        int padLength = (N.bitLength() + 7) / 8;

        byte[] n1_bytes = getPadded(n1, padLength);
        byte[] n2_bytes = getPadded(n2, padLength);

        digest.update(n1_bytes, 0, n1_bytes.length);
        digest.update(n2_bytes, 0, n2_bytes.length);

        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);

        return new BigInteger(1, output).mod(N);
    }

    private static byte[] getPadded(BigInteger n, int length)
    {
        byte[] bs = BigIntegers.asUnsignedByteArray(n);
        if (bs.length < length)
        {
            byte[] tmp = new byte[length];
            System.arraycopy(bs, 0, tmp, length - bs.length, bs.length);
            bs = tmp;
        }
        return bs;
    }
}
