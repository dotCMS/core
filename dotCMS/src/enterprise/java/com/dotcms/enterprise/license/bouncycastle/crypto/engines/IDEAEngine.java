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

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

/**
 * A class that provides a basic International Data Encryption Algorithm (IDEA) engine.
 * <p>
 * This implementation is based on the "HOWTO: INTERNATIONAL DATA ENCRYPTION ALGORITHM"
 * implementation summary by Fauzan Mirza (F.U.Mirza@sheffield.ac.uk). (baring 1 typo at the
 * end of the mulinv function!).
 * <p>
 * It can be found at ftp://ftp.funet.fi/pub/crypt/cryptography/symmetric/idea/
 * <p>
 * Note 1: This algorithm is patented in the USA, Japan, and Europe including
 * at least Austria, France, Germany, Italy, Netherlands, Spain, Sweden, Switzerland
 * and the United Kingdom. Non-commercial use is free, however any commercial
 * products are liable for royalties. Please see
 * <a href="http://www.mediacrypt.com">www.mediacrypt.com</a> for
 * further details. This announcement has been included at the request of
 * the patent holders.
 * <p>
 * Note 2: Due to the requests concerning the above, this algorithm is now only
 * included in the extended Bouncy Castle provider and JCE signed jars. It is
 * not included in the default distributions.
 */
public class IDEAEngine
    implements BlockCipher
{
    protected static final int  BLOCK_SIZE = 8;

    private int[]               workingKey = null;

    /**
     * standard constructor.
     */
    public IDEAEngine()
    {
    }

    /**
     * initialise an IDEA cipher.
     *
     * @param forEncryption whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean           forEncryption,
        CipherParameters  params)
    {
        if (params instanceof KeyParameter)
        {
            workingKey = generateWorkingKey(forEncryption,
                                  ((KeyParameter)params).getKey());
            return;
        }

        throw new IllegalArgumentException("invalid parameter passed to IDEA init - " + params.getClass().getName());
    }

    public String getAlgorithmName()
    {
        return "IDEA";
    }

    public int getBlockSize()
    {
        return BLOCK_SIZE;
    }

    public int processBlock(
        byte[] in,
        int inOff,
        byte[] out,
        int outOff)
    {
        if (workingKey == null)
        {
            throw new IllegalStateException("IDEA engine not initialised");
        }

        if ((inOff + BLOCK_SIZE) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if ((outOff + BLOCK_SIZE) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }

        ideaFunc(workingKey, in, inOff, out, outOff);

        return BLOCK_SIZE;
    }

    public void reset()
    {
    }

    private static final int    MASK = 0xffff;
    private static final int    BASE = 0x10001;

    private int bytesToWord(
        byte[]  in,
        int     inOff)
    {
        return ((in[inOff] << 8) & 0xff00) + (in[inOff + 1] & 0xff);
    }

    private void wordToBytes(
        int     word,
        byte[]  out,
        int     outOff)
    {
        out[outOff] = (byte)(word >>> 8);
        out[outOff + 1] = (byte)word;
    }

    /**
     * return x = x * y where the multiplication is done modulo
     * 65537 (0x10001) (as defined in the IDEA specification) and
     * a zero input is taken to be 65536 (0x10000).
     *
     * @param x the x value
     * @param y the y value
     * @return x = x * y
     */
    private int mul(
        int x,
        int y)
    {
        if (x == 0)
        {
            x = (BASE - y);
        }
        else if (y == 0)
        {
            x = (BASE - x);
        }
        else
        {
            int     p = x * y;

            y = p & MASK;
            x = p >>> 16;
            x = y - x + ((y < x) ? 1 : 0);
        }

        return x & MASK;
    }

    private void ideaFunc(
        int[]   workingKey,
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        int     x0, x1, x2, x3, t0, t1;
        int     keyOff = 0;

        x0 = bytesToWord(in, inOff);
        x1 = bytesToWord(in, inOff + 2);
        x2 = bytesToWord(in, inOff + 4);
        x3 = bytesToWord(in, inOff + 6);

        for (int round = 0; round < 8; round++)
        {
            x0 = mul(x0, workingKey[keyOff++]);
            x1 += workingKey[keyOff++];
            x1 &= MASK;
            x2 += workingKey[keyOff++];
            x2 &= MASK;
            x3 = mul(x3, workingKey[keyOff++]);

            t0 = x1;
            t1 = x2;
            x2 ^= x0;
            x1 ^= x3;

            x2 = mul(x2, workingKey[keyOff++]);
            x1 += x2;
            x1 &= MASK;

            x1 = mul(x1, workingKey[keyOff++]);
            x2 += x1;
            x2 &= MASK;

            x0 ^= x1;
            x3 ^= x2;
            x1 ^= t1;
            x2 ^= t0;
        }

        wordToBytes(mul(x0, workingKey[keyOff++]), out, outOff);
        wordToBytes(x2 + workingKey[keyOff++], out, outOff + 2);  /* NB: Order */
        wordToBytes(x1 + workingKey[keyOff++], out, outOff + 4);
        wordToBytes(mul(x3, workingKey[keyOff]), out, outOff + 6);
    }

    /**
     * The following function is used to expand the user key to the encryption
     * subkey. The first 16 bytes are the user key, and the rest of the subkey
     * is calculated by rotating the previous 16 bytes by 25 bits to the left,
     * and so on until the subkey is completed.
     */
    private int[] expandKey(
        byte[]  uKey)
    {
        int[]   key = new int[52];

        if (uKey.length < 16)
        {
            byte[]  tmp = new byte[16];

            System.arraycopy(uKey, 0, tmp, tmp.length - uKey.length, uKey.length);

            uKey = tmp;
        }

        for (int i = 0; i < 8; i++)
        {
            key[i] = bytesToWord(uKey, i * 2);
        }

        for (int i = 8; i < 52; i++)
        {
            if ((i & 7) < 6)
            {
                key[i] = ((key[i - 7] & 127) << 9 | key[i - 6] >> 7) & MASK;
            }
            else if ((i & 7) == 6)
            {
                key[i] = ((key[i - 7] & 127) << 9 | key[i - 14] >> 7) & MASK;
            }
            else
            {
                key[i] = ((key[i - 15] & 127) << 9 | key[i - 14] >> 7) & MASK;
            }
        }

        return key;
    }

    /**
     * This function computes multiplicative inverse using Euclid's Greatest
     * Common Divisor algorithm. Zero and one are self inverse.
     * <p>
     * i.e. x * mulInv(x) == 1 (modulo BASE)
     */
    private int mulInv(
        int x)
    {
        int t0, t1, q, y;
        
        if (x < 2)
        {
            return x;
        }

        t0 = 1;
        t1 = BASE / x;
        y  = BASE % x;

        while (y != 1)
        {
            q = x / y;
            x = x % y;
            t0 = (t0 + (t1 * q)) & MASK;
            if (x == 1)
            {
                return t0;
            }
            q = y / x;
            y = y % x;
            t1 = (t1 + (t0 * q)) & MASK;
        }

        return (1 - t1) & MASK;
    }

    /**
     * Return the additive inverse of x.
     * <p>
     * i.e. x + addInv(x) == 0
     */
    int addInv(
        int x)
    {
        return (0 - x) & MASK;
    }
    
    /**
     * The function to invert the encryption subkey to the decryption subkey.
     * It also involves the multiplicative inverse and the additive inverse functions.
     */
    private int[] invertKey(
        int[] inKey)
    {
        int     t1, t2, t3, t4;
        int     p = 52;                 /* We work backwards */
        int[]   key = new int[52];
        int     inOff = 0;
    
        t1 = mulInv(inKey[inOff++]);
        t2 = addInv(inKey[inOff++]);
        t3 = addInv(inKey[inOff++]);
        t4 = mulInv(inKey[inOff++]);
        key[--p] = t4;
        key[--p] = t3;
        key[--p] = t2;
        key[--p] = t1;
    
        for (int round = 1; round < 8; round++)
        {
            t1 = inKey[inOff++];
            t2 = inKey[inOff++];
            key[--p] = t2;
            key[--p] = t1;
    
            t1 = mulInv(inKey[inOff++]);
            t2 = addInv(inKey[inOff++]);
            t3 = addInv(inKey[inOff++]);
            t4 = mulInv(inKey[inOff++]);
            key[--p] = t4;
            key[--p] = t2; /* NB: Order */
            key[--p] = t3;
            key[--p] = t1;
        }

        t1 = inKey[inOff++];
        t2 = inKey[inOff++];
        key[--p] = t2;
        key[--p] = t1;
    
        t1 = mulInv(inKey[inOff++]);
        t2 = addInv(inKey[inOff++]);
        t3 = addInv(inKey[inOff++]);
        t4 = mulInv(inKey[inOff]);
        key[--p] = t4;
        key[--p] = t3;
        key[--p] = t2;
        key[--p] = t1;

        return key;
    }
    
    private int[] generateWorkingKey(
        boolean forEncryption,
        byte[]  userKey)
    {
        if (forEncryption)
        {
            return expandKey(userKey);
        }
        else
        {
            return invertKey(expandKey(userKey));
        }
    }
}
