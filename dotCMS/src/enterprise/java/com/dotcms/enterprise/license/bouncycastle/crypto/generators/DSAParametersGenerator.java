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

import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA256Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAValidationParameters;
import com.dotcms.enterprise.license.bouncycastle.util.Arrays;
import com.dotcms.enterprise.license.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;

// TODO Update javadoc to mention FIPS 186-3 when done
/**
 * generate suitable parameters for DSA, in line with FIPS 186-2.
 */
public class DSAParametersGenerator
{
    private int             L, N;
    private int             certainty;
    private SecureRandom    random;

    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    /**
     * initialise the key generator.
     *
     * @param size size of the key (range 2^512 -> 2^1024 - 64 bit increments)
     * @param certainty measure of robustness of prime (for FIPS 186-2 compliance this should be at least 80).
     * @param random random byte source.
     */
    public void init(
        int             size,
        int             certainty,
        SecureRandom    random)
    {
        init(size, getDefaultN(size), certainty, random);
    }

    // TODO Make public to enable support for DSA keys > 1024 bits
    private void init(
        int             L,
        int             N,
        int             certainty,
        SecureRandom    random)
    {
        // TODO Check that the (L, N) pair is in the list of acceptable (L, N pairs) (see Section 4.2)
        // TODO Should we enforce the minimum 'certainty' values as per C.3 Table C.1?

        this.L = L;
        this.N = N;
        this.certainty = certainty;
        this.random = random;
    }

    /**
     * which generates the p and g values from the given parameters,
     * returning the DSAParameters object.
     * <p>
     * Note: can take a while...
     */
    public DSAParameters generateParameters()
    {
        return L > 1024
            ? generateParameters_FIPS186_3()
            : generateParameters_FIPS186_2();
    }

    private DSAParameters generateParameters_FIPS186_2()
    {
        byte[]          seed = new byte[20];
        byte[]          part1 = new byte[20];
        byte[]          part2 = new byte[20];
        byte[]          u = new byte[20];
        SHA1Digest      sha1 = new SHA1Digest();
        int             n = (L - 1) / 160;
        byte[]          w = new byte[L / 8];

        for (;;)
        {
            random.nextBytes(seed);

            hash(sha1, seed, part1);
            System.arraycopy(seed, 0, part2, 0, seed.length);
            inc(part2);
            hash(sha1, part2, part2);

            for (int i = 0; i != u.length; i++)
            {
                u[i] = (byte)(part1[i] ^ part2[i]);
            }

            u[0] |= (byte)0x80;
            u[19] |= (byte)0x01;

            BigInteger q = new BigInteger(1, u);

            if (!q.isProbablePrime(certainty))
            {
                continue;
            }

            byte[] offset = Arrays.clone(seed);
            inc(offset);

            for (int counter = 0; counter < 4096; ++counter)
            {
                for (int k = 0; k < n; k++)
                {
                    inc(offset);
                    hash(sha1, offset, part1);
                    System.arraycopy(part1, 0, w, w.length - (k + 1) * part1.length, part1.length);
                }

                inc(offset);
                hash(sha1, offset, part1);
                System.arraycopy(part1, part1.length - ((w.length - (n) * part1.length)), w, 0, w.length - n * part1.length);

                w[0] |= (byte)0x80;

                BigInteger x = new BigInteger(1, w);

                BigInteger c = x.mod(q.shiftLeft(1));

                BigInteger p = x.subtract(c.subtract(ONE));

                if (p.bitLength() != L)
                {
                    continue;
                }

                if (p.isProbablePrime(certainty))
                {
                    BigInteger g = calculateGenerator_FIPS186_2(p, q, random);

                    return new DSAParameters(p, q, g, new DSAValidationParameters(seed, counter));
                }
            }
        }
    }

    private static BigInteger calculateGenerator_FIPS186_2(BigInteger p, BigInteger q, SecureRandom r)
    {
        BigInteger e = p.subtract(ONE).divide(q);
        BigInteger pSub2 = p.subtract(TWO);

        for (;;)
        {
            BigInteger h = BigIntegers.createRandomInRange(TWO, pSub2, r);
            BigInteger g = h.modPow(e, p);
            if (g.bitLength() > 1)
            {
                return g;
            }
        }
    }

    /**
     * generate suitable parameters for DSA, in line with
     * <i>FIPS 186-3 A.1 Generation of the FFC Primes p and q</i>.
     */
    private DSAParameters generateParameters_FIPS186_3()
    {
// A.1.1.2 Generation of the Probable Primes p and q Using an Approved Hash Function
        // FIXME This should be configurable (digest size in bits must be >= N)
        Digest d = new SHA256Digest();
        int outlen = d.getDigestSize() * 8;

// 1. Check that the (L, N) pair is in the list of acceptable (L, N pairs) (see Section 4.2). If
//    the pair is not in the list, then return INVALID.
        // Note: checked at initialisation

// 2. If (seedlen < N), then return INVALID.
        // FIXME This should be configurable (must be >= N)
        int seedlen = N;
        byte[] seed = new byte[seedlen / 8];

// 3. n = ⎡L ⁄ outlen⎤ – 1.
        int n = (L - 1) / outlen;

// 4. b = L – 1 – (n ∗ outlen).
        int b = (L - 1) % outlen;

        byte[] output = new byte[d.getDigestSize()];
        for (;;)
        {
// 5. Get an arbitrary sequence of seedlen bits as the domain_parameter_seed.
            random.nextBytes(seed);

// 6. U = Hash (domain_parameter_seed) mod 2^(N–1).
            hash(d, seed, output);
            BigInteger U = new BigInteger(1, output).mod(ONE.shiftLeft(N - 1));

// 7. q = 2^(N–1) + U + 1 – ( U mod 2).
            BigInteger q = ONE.shiftLeft(N - 1).add(U).add(ONE).subtract(U.mod(TWO));

// 8. Test whether or not q is prime as specified in Appendix C.3.
            // TODO Review C.3 for primality checking
            if (!q.isProbablePrime(certainty))
            {
// 9. If q is not a prime, then go to step 5.
                continue;
            }

// 10. offset = 1.
            // Note: 'offset' value managed incrementally
            byte[] offset = Arrays.clone(seed);

// 11. For counter = 0 to (4L – 1) do
            int counterLimit = 4 * L;
            for (int counter = 0; counter < counterLimit; ++counter)
            {
// 11.1 For j = 0 to n do
//      Vj = Hash ((domain_parameter_seed + offset + j) mod 2^seedlen).
// 11.2 W = V0 + (V1 ∗ 2^outlen) + ... + (V^(n–1) ∗ 2^((n–1) ∗ outlen)) + ((Vn mod 2^b) ∗ 2^(n ∗ outlen)).
                // TODO Assemble w as a byte array
                BigInteger W = ZERO;
                for (int j = 0, exp = 0; j <= n; ++j, exp += outlen)
                {
                    inc(offset);
                    hash(d, offset, output);

                    BigInteger Vj = new BigInteger(1, output);
                    if (j == n)
                    {
                        Vj = Vj.mod(ONE.shiftLeft(b));
                    }

                    W = W.add(Vj.shiftLeft(exp));
                }

// 11.3 X = W + 2^(L–1). Comment: 0 ≤ W < 2L–1; hence, 2L–1 ≤ X < 2L.
                BigInteger X = W.add(ONE.shiftLeft(L - 1));
 
// 11.4 c = X mod 2q.
                BigInteger c = X.mod(q.shiftLeft(1));

// 11.5 p = X - (c - 1). Comment: p ≡ 1 (mod 2q).
                BigInteger p = X.subtract(c.subtract(ONE));

// 11.6 If (p < 2^(L - 1)), then go to step 11.9
                if (p.bitLength() != L)
                {
                    continue;
                }

// 11.7 Test whether or not p is prime as specified in Appendix C.3.
                // TODO Review C.3 for primality checking
                if (p.isProbablePrime(certainty))
                {
// 11.8 If p is determined to be prime, then return VALID and the values of p, q and
//      (optionally) the values of domain_parameter_seed and counter.
                    // TODO Make configurable (8-bit unsigned)?
//                    int index = 1;
//                    BigInteger g = calculateGenerator_FIPS186_3_Verifiable(d, p, q, seed, index);
//                    if (g != null)
//                    {
//                        // TODO Should 'index' be a part of the validation parameters?
//                        return new DSAParameters(p, q, g, new DSAValidationParameters(seed, counter));
//                    }

                    BigInteger g = calculateGenerator_FIPS186_3_Unverifiable(p, q, random);
                    return new DSAParameters(p, q, g, new DSAValidationParameters(seed, counter));
                }

// 11.9 offset = offset + n + 1.      Comment: Increment offset; then, as part of
//                                    the loop in step 11, increment counter; if
//                                    counter < 4L, repeat steps 11.1 through 11.8.
                // Note: 'offset' value already incremented in inner loop
            }
// 12. Go to step 5.
        }
    }

    private static BigInteger calculateGenerator_FIPS186_3_Unverifiable(BigInteger p, BigInteger q,
        SecureRandom r)
    {
        return calculateGenerator_FIPS186_2(p, q, r);
    }

//    private static BigInteger calculateGenerator_FIPS186_3_Verifiable(Digest d, BigInteger p, BigInteger q,
//        byte[] seed, int index)
//    {
//// A.2.3 Verifiable Canonical Generation of the Generator g
//        BigInteger e = p.subtract(ONE).divide(q);
//        byte[] ggen = Hex.decode("6767656E");
//
//        // 7. U = domain_parameter_seed || “ggen” || index || count.
//        byte[] U = new byte[seed.length + ggen.length + 1 + 2];
//        System.arraycopy(seed, 0, U, 0, seed.length);
//        System.arraycopy(ggen, 0, U, seed.length, ggen.length);
//        U[U.length - 3] = (byte)index; 
//
//        byte[] w = new byte[d.getDigestSize()];
//        for (int count = 1; count < (1 << 16); ++count)
//        {
//            inc(U);
//            hash(d, U, w);
//            BigInteger W = new BigInteger(1, w);
//            BigInteger g = W.modPow(e, p);
//            if (g.compareTo(TWO) >= 0)
//            {
//                return g;
//            }
//        }
//
//        return null;
//    }

    private static void hash(Digest d, byte[] input, byte[] output)
    {
        d.update(input, 0, input.length);
        d.doFinal(output, 0);
    }

    private static int getDefaultN(int L)
    {
        return L > 1024 ? 256 : 160;
    }

    private static void inc(byte[] buf)
    {
        for (int i = buf.length - 1; i >= 0; --i)
        {
            byte b = (byte)((buf[i] + 1) & 0xff);
            buf[i] = b;

            if (b != 0)
            {
                break;
            }
        }
    }
}
