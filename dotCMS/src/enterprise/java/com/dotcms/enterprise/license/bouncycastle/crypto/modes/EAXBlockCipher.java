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

package com.dotcms.enterprise.license.bouncycastle.crypto.modes;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.macs.CMac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.AEADParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;
import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

/**
 * A Two-Pass Authenticated-Encryption Scheme Optimized for Simplicity and 
 * Efficiency - by M. Bellare, P. Rogaway, D. Wagner.
 * 
 * http://www.cs.ucdavis.edu/~rogaway/papers/eax.pdf
 * 
 * EAX is an AEAD scheme based on CTR and OMAC1/CMAC, that uses a single block 
 * cipher to encrypt and authenticate data. It's on-line (the length of a 
 * message isn't needed to begin processing it), has good performances, it's
 * simple and provably secure (provided the underlying block cipher is secure).
 * 
 * Of course, this implementations is NOT thread-safe.
 */
public class EAXBlockCipher
    implements AEADBlockCipher
{
    private static final byte nTAG = 0x0;

    private static final byte hTAG = 0x1;

    private static final byte cTAG = 0x2;

    private SICBlockCipher cipher;

    private boolean forEncryption;

    private int blockSize;

    private Mac mac;

    private byte[] nonceMac;
    private byte[] associatedTextMac;
    private byte[] macBlock;
    
    private int macSize;
    private byte[] bufBlock;
    private int bufOff;

    /**
     * Constructor that accepts an instance of a block cipher engine.
     *
     * @param cipher the engine to use
     */
    public EAXBlockCipher(BlockCipher cipher)
    {
        blockSize = cipher.getBlockSize();
        mac = new CMac(cipher);
        macBlock = new byte[blockSize];
        bufBlock = new byte[blockSize * 2];
        associatedTextMac = new byte[mac.getMacSize()];
        nonceMac = new byte[mac.getMacSize()];
        this.cipher = new SICBlockCipher(cipher);
    }

    public String getAlgorithmName()
    {
        return cipher.getUnderlyingCipher().getAlgorithmName() + "/EAX";
    }

    public BlockCipher getUnderlyingCipher()
    {
        return cipher.getUnderlyingCipher();
    }

    public int getBlockSize()
    {
        return cipher.getBlockSize();
    }

    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException
    {
        this.forEncryption = forEncryption;

        byte[] nonce, associatedText;
        CipherParameters keyParam;

        if (params instanceof AEADParameters)
        {
            AEADParameters param = (AEADParameters)params;

            nonce = param.getNonce();
            associatedText = param.getAssociatedText();
            macSize = param.getMacSize() / 8;
            keyParam = param.getKey();
        }
        else if (params instanceof ParametersWithIV)
        {
            ParametersWithIV param = (ParametersWithIV)params;

            nonce = param.getIV();
            associatedText = new byte[0];
            macSize = mac.getMacSize() / 2;
            keyParam = param.getParameters();
        }
        else
        {
            throw new IllegalArgumentException("invalid parameters passed to EAX");
        }

        byte[] tag = new byte[blockSize];

        mac.init(keyParam);
        tag[blockSize - 1] = hTAG;
        mac.update(tag, 0, blockSize);
        mac.update(associatedText, 0, associatedText.length);
        mac.doFinal(associatedTextMac, 0);

        tag[blockSize - 1] = nTAG;
        mac.update(tag, 0, blockSize);
        mac.update(nonce, 0, nonce.length);
        mac.doFinal(nonceMac, 0);

        tag[blockSize - 1] = cTAG;
        mac.update(tag, 0, blockSize);

        cipher.init(true, new ParametersWithIV(keyParam, nonceMac));
    }

    private void calculateMac()
    {
        byte[] outC = new byte[blockSize];
        mac.doFinal(outC, 0);

        for (int i = 0; i < macBlock.length; i++)
        {
            macBlock[i] = (byte)(nonceMac[i] ^ associatedTextMac[i] ^ outC[i]);
        }
    }

    public void reset()
    {
        reset(true);
    }

    private void reset(
        boolean clearMac)
    {
        cipher.reset();
        mac.reset();

        bufOff = 0;
        Arrays.fill(bufBlock, (byte)0);

        if (clearMac)
        {
            Arrays.fill(macBlock, (byte)0);
        }

        byte[] tag = new byte[blockSize];
        tag[blockSize - 1] = cTAG;
        mac.update(tag, 0, blockSize);
    }

    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException
    {
        return process(in, out, outOff);
    }

    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff)
        throws DataLengthException
    {
        int resultLen = 0;

        for (int i = 0; i != len; i++)
        {
            resultLen += process(in[inOff + i], out, outOff + resultLen);
        }

        return resultLen;
    }

    public int doFinal(byte[] out, int outOff)
        throws IllegalStateException, InvalidCipherTextException
    {
        int extra = bufOff;
        byte[] tmp = new byte[bufBlock.length];

        bufOff = 0;

        if (forEncryption)
        {
            cipher.processBlock(bufBlock, 0, tmp, 0);
            cipher.processBlock(bufBlock, blockSize, tmp, blockSize);

            System.arraycopy(tmp, 0, out, outOff, extra);

            mac.update(tmp, 0, extra);

            calculateMac();

            System.arraycopy(macBlock, 0, out, outOff + extra, macSize);

            reset(false);

            return extra + macSize;
        }
        else
        {
            if (extra > macSize)
            {
                mac.update(bufBlock, 0, extra - macSize);

                cipher.processBlock(bufBlock, 0, tmp, 0);
                cipher.processBlock(bufBlock, blockSize, tmp, blockSize);

                System.arraycopy(tmp, 0, out, outOff, extra - macSize);
            }

            calculateMac();

            if (!verifyMac(bufBlock, extra - macSize))
            {
                throw new InvalidCipherTextException("mac check in EAX failed");
            }

            reset(false);

            return extra - macSize;
        }
    }

    public byte[] getMac()
    {
        byte[] mac = new byte[macSize];

        System.arraycopy(macBlock, 0, mac, 0, macSize);

        return mac;
    }

    public int getUpdateOutputSize(int len)
    {
        return ((len + bufOff) / blockSize) * blockSize;
    }

    public int getOutputSize(int len)
    {
        if (forEncryption)
        {
             return len + bufOff + macSize;
        }
        else
        {
             return len + bufOff - macSize;
        }
    }

    private int process(byte b, byte[] out, int outOff)
    {
        bufBlock[bufOff++] = b;

        if (bufOff == bufBlock.length)
        {
            int size;

            if (forEncryption)
            {
                size = cipher.processBlock(bufBlock, 0, out, outOff);

                mac.update(out, outOff, blockSize);
            }
            else
            {
                mac.update(bufBlock, 0, blockSize);

                size = cipher.processBlock(bufBlock, 0, out, outOff);
            }

            bufOff = blockSize;
            System.arraycopy(bufBlock, blockSize, bufBlock, 0, blockSize);

            return size;
        }

        return 0;
    }

    private boolean verifyMac(byte[] mac, int off)
    {
        for (int i = 0; i < macSize; i++)
        {
            if (macBlock[i] != mac[off + i])
            {
                return false;
            }
        }

        return true;
    }
}
