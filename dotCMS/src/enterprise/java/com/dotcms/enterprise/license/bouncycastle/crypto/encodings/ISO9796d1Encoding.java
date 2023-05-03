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

package com.dotcms.enterprise.license.bouncycastle.crypto.encodings;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;

/**
 * ISO 9796-1 padding. Note in the light of recent results you should
 * only use this with RSA (rather than the "simpler" Rabin keys) and you
 * should never use it with anything other than a hash (ie. even if the 
 * message is small don't sign the message, sign it's hash) or some "random"
 * value. See your favorite search engine for details.
 */
public class ISO9796d1Encoding
    implements AsymmetricBlockCipher
{
    private static byte[]    shadows = { 0xe, 0x3, 0x5, 0x8, 0x9, 0x4, 0x2, 0xf,
                                    0x0, 0xd, 0xb, 0x6, 0x7, 0xa, 0xc, 0x1 };
    private static byte[]    inverse = { 0x8, 0xf, 0x6, 0x1, 0x5, 0x2, 0xb, 0xc,
                                    0x3, 0x4, 0xd, 0xa, 0xe, 0x9, 0x0, 0x7 };

    private AsymmetricBlockCipher   engine;
    private boolean                 forEncryption;
    private int                     bitSize;
    private int                     padBits = 0;

    public ISO9796d1Encoding(
        AsymmetricBlockCipher   cipher)
    {
        this.engine = cipher;
    }   

    public AsymmetricBlockCipher getUnderlyingCipher()
    {
        return engine;
    }

    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        RSAKeyParameters  kParam = null;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            kParam = (RSAKeyParameters)rParam.getParameters();
        }
        else
        {
            kParam = (RSAKeyParameters)param;
        }

        engine.init(forEncryption, param);

        bitSize = kParam.getModulus().bitLength();

        this.forEncryption = forEncryption;
    }

    /**
     * return the input block size. The largest message we can process
     * is (key_size_in_bits + 3)/16, which in our world comes to 
     * key_size_in_bytes / 2.
     */
    public int getInputBlockSize()
    {
        int     baseBlockSize = engine.getInputBlockSize();

        if (forEncryption)
        {
            return (baseBlockSize + 1) / 2;
        }
        else
        {
            return baseBlockSize;
        }
    }

    /**
     * return the maximum possible size for the output.
     */
    public int getOutputBlockSize()
    {
        int     baseBlockSize = engine.getOutputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize;
        }
        else
        {
            return (baseBlockSize + 1) / 2;
        }
    }

    /**
     * set the number of bits in the next message to be treated as 
     * pad bits.
     */
    public void setPadBits(
        int     padBits)
    {
        if (padBits > 7)
        {
            throw new IllegalArgumentException("padBits > 7");
        }

        this.padBits = padBits;
    }

    /**
     * retrieve the number of pad bits in the last decoded message.
     */
    public int getPadBits()
    {
        return padBits;
    }

    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (forEncryption)
        {
            return encodeBlock(in, inOff, inLen);
        }
        else
        {
            return decodeBlock(in, inOff, inLen);
        }
    }

    private byte[] encodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        byte[]  block = new byte[(bitSize + 7) / 8];
        int     r = padBits + 1;
        int     z = inLen;
        int     t = (bitSize + 13) / 16;

        for (int i = 0; i < t; i += z)
        {
            if (i > t - z)
            {
                System.arraycopy(in, inOff + inLen - (t - i),
                                    block, block.length - t, t - i);
            }
            else
            {
                System.arraycopy(in, inOff, block, block.length - (i + z), z);
            }
        }

        for (int i = block.length - 2 * t; i != block.length; i += 2)
        {
            byte    val = block[block.length - t + i / 2];
            
            block[i] = (byte)((shadows[(val & 0xff) >>> 4] << 4)
                                                | shadows[val & 0x0f]);
            block[i + 1] = val;
        }

        block[block.length - 2 * z] ^= r;
        block[block.length - 1] = (byte)((block[block.length - 1] << 4) | 0x06);

        int maxBit = (8 - (bitSize - 1) % 8);
        int offSet = 0;

        if (maxBit != 8)
        {
            block[0] &= 0xff >>> maxBit;
            block[0] |= 0x80 >>> maxBit;
        }
        else
        {
            block[0] = 0x00;
            block[1] |= 0x80;
            offSet = 1;
        }

        return engine.processBlock(block, offSet, block.length - offSet);
    }

    /**
     * @exception InvalidCipherTextException if the decrypted block is not a valid ISO 9796 bit string
     */
    private byte[] decodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        byte[]  block = engine.processBlock(in, inOff, inLen);
        int     r = 1;
        int     t = (bitSize + 13) / 16;

        if ((block[block.length - 1] & 0x0f) != 0x6)
        {
            throw new InvalidCipherTextException("invalid forcing byte in block");
        }

        block[block.length - 1] = (byte)(((block[block.length - 1] & 0xff) >>> 4) | ((inverse[(block[block.length - 2] & 0xff) >> 4]) << 4));
        block[0] = (byte)((shadows[(block[1] & 0xff) >>> 4] << 4)
                                                | shadows[block[1] & 0x0f]);

        boolean boundaryFound = false;
        int     boundary = 0;
        
        for (int i = block.length - 1; i >= block.length - 2 * t; i -= 2)
        {
            int val = ((shadows[(block[i] & 0xff) >>> 4] << 4)
                                        | shadows[block[i] & 0x0f]);
            
            if (((block[i - 1] ^ val) & 0xff) != 0)
            {
                if (!boundaryFound)
                {
                    boundaryFound = true;
                    r = (block[i - 1] ^ val) & 0xff;
                    boundary = i - 1;
                }
                else
                {
                    throw new InvalidCipherTextException("invalid tsums in block");
                }
            }
        }

        block[boundary] = 0;

        byte[]  nblock = new byte[(block.length - boundary) / 2];

        for (int i = 0; i < nblock.length; i++)
        {
            nblock[i] = block[2 * i + boundary + 1];
        }

        padBits = r - 1;

        return nblock;
    }
}
