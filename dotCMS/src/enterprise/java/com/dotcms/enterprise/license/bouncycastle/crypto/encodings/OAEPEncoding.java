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
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;

import java.security.SecureRandom;

/**
 * Optimal Asymmetric Encryption Padding (OAEP) - see PKCS 1 V 2.
 */
public class OAEPEncoding
    implements AsymmetricBlockCipher
{
    private byte[]                  defHash;
    private Digest                  hash;
    private Digest                  mgf1Hash;

    private AsymmetricBlockCipher   engine;
    private SecureRandom            random;
    private boolean                 forEncryption;

    public OAEPEncoding(
        AsymmetricBlockCipher   cipher)
    {
        this(cipher, new SHA1Digest(), null);
    }
    
    public OAEPEncoding(
        AsymmetricBlockCipher       cipher,
        Digest                      hash)
    {
        this(cipher, hash, null);
    }
    
    public OAEPEncoding(
        AsymmetricBlockCipher       cipher,
        Digest                      hash,
        byte[]                      encodingParams)
    {
        this(cipher, hash, hash, encodingParams);
    }

    public OAEPEncoding(
        AsymmetricBlockCipher       cipher,
        Digest                      hash,
        Digest                      mgf1Hash,
        byte[]                      encodingParams)
    {
        this.engine = cipher;
        this.hash = hash;
        this.mgf1Hash = mgf1Hash;
        this.defHash = new byte[hash.getDigestSize()];

        if (encodingParams != null)
        {
            hash.update(encodingParams, 0, encodingParams.length);
        }

        hash.doFinal(defHash, 0);
    }

    public AsymmetricBlockCipher getUnderlyingCipher()
    {
        return engine;
    }

    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom  rParam = (ParametersWithRandom)param;

            this.random = rParam.getRandom();
        }
        else
        {   
            this.random = new SecureRandom();
        }

        engine.init(forEncryption, param);

        this.forEncryption = forEncryption;
    }

    public int getInputBlockSize()
    {
        int     baseBlockSize = engine.getInputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize - 1 - 2 * defHash.length;
        }
        else
        {
            return baseBlockSize;
        }
    }

    public int getOutputBlockSize()
    {
        int     baseBlockSize = engine.getOutputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize;
        }
        else
        {
            return baseBlockSize - 1 - 2 * defHash.length;
        }
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

    public byte[] encodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        byte[]  block = new byte[getInputBlockSize() + 1 + 2 * defHash.length];

        //
        // copy in the message
        //
        System.arraycopy(in, inOff, block, block.length - inLen, inLen);

        //
        // add sentinel
        //
        block[block.length - inLen - 1] = 0x01;

        //
        // as the block is already zeroed - there's no need to add PS (the >= 0 pad of 0)
        //

        //
        // add the hash of the encoding params.
        //
        System.arraycopy(defHash, 0, block, defHash.length, defHash.length);

        //
        // generate the seed.
        //
        byte[]  seed = new byte[defHash.length];

        random.nextBytes(seed);

        //
        // mask the message block.
        //
        byte[]  mask = maskGeneratorFunction1(seed, 0, seed.length, block.length - defHash.length);

        for (int i = defHash.length; i != block.length; i++)
        {
            block[i] ^= mask[i - defHash.length];
        }

        //
        // add in the seed
        //
        System.arraycopy(seed, 0, block, 0, defHash.length);

        //
        // mask the seed.
        //
        mask = maskGeneratorFunction1(
                        block, defHash.length, block.length - defHash.length, defHash.length);

        for (int i = 0; i != defHash.length; i++)
        {
            block[i] ^= mask[i];
        }

        return engine.processBlock(block, 0, block.length);
    }

    /**
     * @exception InvalidCipherTextException if the decrypted block turns out to
     * be badly formatted.
     */
    public byte[] decodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        byte[]  data = engine.processBlock(in, inOff, inLen);
        byte[]  block;

        //
        // as we may have zeros in our leading bytes for the block we produced
        // on encryption, we need to make sure our decrypted block comes back
        // the same size.
        //
        if (data.length < engine.getOutputBlockSize())
        {
            block = new byte[engine.getOutputBlockSize()];

            System.arraycopy(data, 0, block, block.length - data.length, data.length);
        }
        else
        {
            block = data;
        }

        if (block.length < (2 * defHash.length) + 1)
        {
            throw new InvalidCipherTextException("data too short");
        }

        //
        // unmask the seed.
        //
        byte[] mask = maskGeneratorFunction1(
                    block, defHash.length, block.length - defHash.length, defHash.length);

        for (int i = 0; i != defHash.length; i++)
        {
            block[i] ^= mask[i];
        }

        //
        // unmask the message block.
        //
        mask = maskGeneratorFunction1(block, 0, defHash.length, block.length - defHash.length);

        for (int i = defHash.length; i != block.length; i++)
        {
            block[i] ^= mask[i - defHash.length];
        }

        //
        // check the hash of the encoding params.
        //
        for (int i = 0; i != defHash.length; i++)
        {
            if (defHash[i] != block[defHash.length + i])
            {
                throw new InvalidCipherTextException("data hash wrong");
            }
        }

        //
        // find the data block
        //
        int start;

        for (start = 2 * defHash.length; start != block.length; start++)
        {
            if (block[start] != 0)
            {
                break;
            }
        }

        if (start >= (block.length - 1) || block[start] != 1)
        {
            throw new InvalidCipherTextException("data start wrong " + start);
        }

        start++;

        //
        // extract the data block
        //
        byte[]  output = new byte[block.length - start];

        System.arraycopy(block, start, output, 0, output.length);

        return output;
    }

    /**
     * int to octet string.
     */
    private void ItoOSP(
        int     i,
        byte[]  sp)
    {
        sp[0] = (byte)(i >>> 24);
        sp[1] = (byte)(i >>> 16);
        sp[2] = (byte)(i >>> 8);
        sp[3] = (byte)(i >>> 0);
    }

    /**
     * mask generator function, as described in PKCS1v2.
     */
    private byte[] maskGeneratorFunction1(
        byte[]  Z,
        int     zOff,
        int     zLen,
        int     length)
    {
        byte[]  mask = new byte[length];
        byte[]  hashBuf = new byte[mgf1Hash.getDigestSize()];
        byte[]  C = new byte[4];
        int     counter = 0;

        hash.reset();

        do
        {
            ItoOSP(counter, C);

            mgf1Hash.update(Z, zOff, zLen);
            mgf1Hash.update(C, 0, C.length);
            mgf1Hash.doFinal(hashBuf, 0);

            System.arraycopy(hashBuf, 0, mask, counter * hashBuf.length, hashBuf.length);
        }
        while (++counter < (length / hashBuf.length));

        if ((counter * hashBuf.length) < length)
        {
            ItoOSP(counter, C);

            mgf1Hash.update(Z, zOff, zLen);
            mgf1Hash.update(C, 0, C.length);
            mgf1Hash.doFinal(hashBuf, 0);

            System.arraycopy(hashBuf, 0, mask, counter * hashBuf.length, mask.length - (counter * hashBuf.length));
        }

        return mask;
    }
}
