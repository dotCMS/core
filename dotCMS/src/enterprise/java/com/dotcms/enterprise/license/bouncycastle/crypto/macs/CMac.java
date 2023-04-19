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

package com.dotcms.enterprise.license.bouncycastle.crypto.macs;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.modes.CBCBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.paddings.ISO7816d4Padding;

/**
 * CMAC - as specified at www.nuee.nagoya-u.ac.jp/labs/tiwata/omac/omac.html
 * <p>
 * CMAC is analogous to OMAC1 - see also en.wikipedia.org/wiki/CMAC
 * </p><p>
 * CMAC is a NIST recomendation - see 
 * csrc.nist.gov/CryptoToolkit/modes/800-38_Series_Publications/SP800-38B.pdf
 * </p><p>
 * CMAC/OMAC1 is a blockcipher-based message authentication code designed and
 * analyzed by Tetsu Iwata and Kaoru Kurosawa.
 * </p><p>
 * CMAC/OMAC1 is a simple variant of the CBC MAC (Cipher Block Chaining Message 
 * Authentication Code). OMAC stands for One-Key CBC MAC.
 * </p><p>
 * It supports 128- or 64-bits block ciphers, with any key size, and returns
 * a MAC with dimension less or equal to the block size of the underlying 
 * cipher.
 * </p>
 */
public class CMac implements Mac
{
    private static final byte CONSTANT_128 = (byte)0x87;
    private static final byte CONSTANT_64 = (byte)0x1b;

    private byte[] ZEROES;

    private byte[] mac;

    private byte[] buf;
    private int bufOff;
    private BlockCipher cipher;

    private int macSize;

    private byte[] L, Lu, Lu2;

    /**
     * create a standard MAC based on a CBC block cipher (64 or 128 bit block).
     * This will produce an authentication code the length of the block size
     * of the cipher.
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     */
    public CMac(BlockCipher cipher)
    {
        this(cipher, cipher.getBlockSize() * 8);
    }

    /**
     * create a standard MAC based on a block cipher with the size of the
     * MAC been given in bits.
     * <p/>
     * Note: the size of the MAC must be at least 24 bits (FIPS Publication 81),
     * or 16 bits if being used as a data authenticator (FIPS Publication 113),
     * and in general should be less than the size of the block cipher as it reduces
     * the chance of an exhaustive attack (see Handbook of Applied Cryptography).
     *
     * @param cipher        the cipher to be used as the basis of the MAC generation.
     * @param macSizeInBits the size of the MAC in bits, must be a multiple of 8 and <= 128.
     */
    public CMac(BlockCipher cipher, int macSizeInBits)
    {
        if ((macSizeInBits % 8) != 0)
        {
            throw new IllegalArgumentException("MAC size must be multiple of 8");
        }

        if (macSizeInBits > (cipher.getBlockSize() * 8))
        {
            throw new IllegalArgumentException(
                "MAC size must be less or equal to "
                    + (cipher.getBlockSize() * 8));
        }

        if (cipher.getBlockSize() != 8 && cipher.getBlockSize() != 16)
        {
            throw new IllegalArgumentException(
                "Block size must be either 64 or 128 bits");
        }

        this.cipher = new CBCBlockCipher(cipher);
        this.macSize = macSizeInBits / 8;

        mac = new byte[cipher.getBlockSize()];

        buf = new byte[cipher.getBlockSize()];

        ZEROES = new byte[cipher.getBlockSize()];

        bufOff = 0;
    }

    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }

    private byte[] doubleLu(byte[] in)
    {
        int FirstBit = (in[0] & 0xFF) >> 7;
        byte[] ret = new byte[in.length];
        for (int i = 0; i < in.length - 1; i++)
        {
            ret[i] = (byte)((in[i] << 1) + ((in[i + 1] & 0xFF) >> 7));
        }
        ret[in.length - 1] = (byte)(in[in.length - 1] << 1);
        if (FirstBit == 1)
        {
            ret[in.length - 1] ^= in.length == 16 ? CONSTANT_128 : CONSTANT_64;
        }
        return ret;
    }

    public void init(CipherParameters params)
    {
        reset();

        cipher.init(true, params);

        //initializes the L, Lu, Lu2 numbers
        L = new byte[ZEROES.length];
        cipher.processBlock(ZEROES, 0, L, 0);
        Lu = doubleLu(L);
        Lu2 = doubleLu(Lu);

        cipher.init(true, params);
    }

    public int getMacSize()
    {
        return macSize;
    }

    public void update(byte in)
    {
        if (bufOff == buf.length)
        {
            cipher.processBlock(buf, 0, mac, 0);
            bufOff = 0;
        }

        buf[bufOff++] = in;
    }

    public void update(byte[] in, int inOff, int len)
    {
        if (len < 0)
        {
            throw new IllegalArgumentException(
                "Can't have a negative input length!");
        }

        int blockSize = cipher.getBlockSize();
        int gapLen = blockSize - bufOff;

        if (len > gapLen)
        {
            System.arraycopy(in, inOff, buf, bufOff, gapLen);

            cipher.processBlock(buf, 0, mac, 0);

            bufOff = 0;
            len -= gapLen;
            inOff += gapLen;

            while (len > blockSize)
            {
                cipher.processBlock(in, inOff, mac, 0);

                len -= blockSize;
                inOff += blockSize;
            }
        }

        System.arraycopy(in, inOff, buf, bufOff, len);

        bufOff += len;
    }

    public int doFinal(byte[] out, int outOff)
    {
        int blockSize = cipher.getBlockSize();

        byte[] lu;
        if (bufOff == blockSize)
        {
            lu = Lu;
        }
        else
        {
            new ISO7816d4Padding().addPadding(buf, bufOff);
            lu = Lu2;
        }

        for (int i = 0; i < mac.length; i++)
        {
            buf[i] ^= lu[i];
        }

        cipher.processBlock(buf, 0, mac, 0);

        System.arraycopy(mac, 0, out, outOff, macSize);

        reset();

        return macSize;
    }

    /**
     * Reset the mac generator.
     */
    public void reset()
    {
        /*
         * clean the buffer.
         */
        for (int i = 0; i < buf.length; i++)
        {
            buf[i] = 0;
        }

        bufOff = 0;

        /*
         * reset the underlying cipher.
         */
        cipher.reset();
    }
}
