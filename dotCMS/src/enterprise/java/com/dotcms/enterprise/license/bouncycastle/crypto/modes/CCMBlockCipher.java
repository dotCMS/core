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

import java.io.ByteArrayOutputStream;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.macs.CBCBlockCipherMac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.AEADParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;
import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

/**
 * Implements the Counter with Cipher Block Chaining mode (CCM) detailed in
 * NIST Special Publication 800-38C.
 * <p>
 * <b>Note</b>: this mode is a packet mode - it needs all the data up front.
 */
public class CCMBlockCipher
    implements AEADBlockCipher
{
    private BlockCipher           cipher;
    private int                   blockSize;
    private boolean               forEncryption;
    private byte[]                nonce;
    private byte[]                associatedText;
    private int                   macSize;
    private CipherParameters      keyParam;
    private byte[]                macBlock;
    private ByteArrayOutputStream data = new ByteArrayOutputStream();

    /**
     * Basic constructor.
     *
     * @param c the block cipher to be used.
     */
    public CCMBlockCipher(BlockCipher c)
    {
        this.cipher = c;
        this.blockSize = c.getBlockSize();
        this.macBlock = new byte[blockSize];
        
        if (blockSize != 16)
        {
            throw new IllegalArgumentException("cipher required with a block size of 16.");
        }
    }

    /**
     * return the underlying block cipher that we are wrapping.
     *
     * @return the underlying block cipher that we are wrapping.
     */
    public BlockCipher getUnderlyingCipher()
    {
        return cipher;
    }


    public void init(boolean forEncryption, CipherParameters params)
          throws IllegalArgumentException
    {
        this.forEncryption = forEncryption;

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
            associatedText = null;
            macSize = macBlock.length / 2;
            keyParam = param.getParameters();
        }
        else
        {
            throw new IllegalArgumentException("invalid parameters passed to CCM");
        }
    }

    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName() + "/CCM";
    }

    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {
        data.write(in);

        return 0;
    }

    public int processBytes(byte[] in, int inOff, int inLen, byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {
        data.write(in, inOff, inLen);

        return 0;
    }

    public int doFinal(byte[] out, int outOff)
        throws IllegalStateException, InvalidCipherTextException
    {
        byte[] text = data.toByteArray();
        byte[] enc = processPacket(text, 0, text.length);

        System.arraycopy(enc, 0, out, outOff, enc.length);

        reset();

        return enc.length;
    }

    public void reset()
    {
        cipher.reset();
        data.reset();
    }

    /**
     * Returns a byte array containing the mac calculated as part of the
     * last encrypt or decrypt operation.
     * 
     * @return the last mac calculated.
     */
    public byte[] getMac()
    {
        byte[] mac = new byte[macSize];
        
        System.arraycopy(macBlock, 0, mac, 0, mac.length);
        
        return mac;
    }

    public int getUpdateOutputSize(int len)
    {
        return 0;
    }

    public int getOutputSize(int len)
    {
        if (forEncryption)
        {
            return data.size() + len + macSize;
        }
        else
        {
            return data.size() + len - macSize;
        }
    }

    public byte[] processPacket(byte[] in, int inOff, int inLen)
        throws IllegalStateException, InvalidCipherTextException
    {
        if (keyParam == null)
        {
            throw new IllegalStateException("CCM cipher unitialized.");
        }
        
        BlockCipher ctrCipher = new SICBlockCipher(cipher);
        byte[] iv = new byte[blockSize];
        byte[] out;

        iv[0] = (byte)(((15 - nonce.length) - 1) & 0x7);
        
        System.arraycopy(nonce, 0, iv, 1, nonce.length);
        
        ctrCipher.init(forEncryption, new ParametersWithIV(keyParam, iv));
        
        if (forEncryption)
        {
            int index = inOff;
            int outOff = 0;
            
            out = new byte[inLen + macSize];
            
            calculateMac(in, inOff, inLen, macBlock);
            
            ctrCipher.processBlock(macBlock, 0, macBlock, 0);   // S0
            
            while (index < inLen - blockSize)                   // S1...
            {
                ctrCipher.processBlock(in, index, out, outOff);
                outOff += blockSize;
                index += blockSize;
            }
            
            byte[] block = new byte[blockSize];
            
            System.arraycopy(in, index, block, 0, inLen - index);
            
            ctrCipher.processBlock(block, 0, block, 0);
            
            System.arraycopy(block, 0, out, outOff, inLen - index);
            
            outOff += inLen - index;

            System.arraycopy(macBlock, 0, out, outOff, out.length - outOff);
        }
        else
        {
            int index = inOff;
            int outOff = 0;
            
            out = new byte[inLen - macSize];
            
            System.arraycopy(in, inOff + inLen - macSize, macBlock, 0, macSize);
            
            ctrCipher.processBlock(macBlock, 0, macBlock, 0);
            
            for (int i = macSize; i != macBlock.length; i++)
            {
                macBlock[i] = 0;
            }
            
            while (outOff < out.length - blockSize)
            {
                ctrCipher.processBlock(in, index, out, outOff);
                outOff += blockSize;
                index += blockSize;
            }
            
            byte[] block = new byte[blockSize];
            
            System.arraycopy(in, index, block, 0, out.length - outOff);
            
            ctrCipher.processBlock(block, 0, block, 0);
            
            System.arraycopy(block, 0, out, outOff, out.length - outOff);
            
            byte[] calculatedMacBlock = new byte[blockSize];
            
            calculateMac(out, 0, out.length, calculatedMacBlock);
            
            if (!Arrays.constantTimeAreEqual(macBlock, calculatedMacBlock))
            {
                throw new InvalidCipherTextException("mac check in CCM failed");
            }
        }
        
        return out;
    }
    
    private int calculateMac(byte[] data, int dataOff, int dataLen, byte[] macBlock)
    {
        Mac    cMac = new CBCBlockCipherMac(cipher, macSize * 8);

        cMac.init(keyParam);

        //
        // build b0
        //
        byte[] b0 = new byte[16];
    
        if (hasAssociatedText())
        {
            b0[0] |= 0x40;
        }
        
        b0[0] |= (((cMac.getMacSize() - 2) / 2) & 0x7) << 3;

        b0[0] |= ((15 - nonce.length) - 1) & 0x7;
        
        System.arraycopy(nonce, 0, b0, 1, nonce.length);
        
        int q = dataLen;
        int count = 1;
        while (q > 0)
        {
            b0[b0.length - count] = (byte)(q & 0xff);
            q >>>= 8;
            count++;
        }
        
        cMac.update(b0, 0, b0.length);
        
        //
        // process associated text
        //
        if (hasAssociatedText())
        {
            int extra;
            
            if (associatedText.length < ((1 << 16) - (1 << 8)))
            {
                cMac.update((byte)(associatedText.length >> 8));
                cMac.update((byte)associatedText.length);
                
                extra = 2;
            }
            else // can't go any higher than 2^32
            {
                cMac.update((byte)0xff);
                cMac.update((byte)0xfe);
                cMac.update((byte)(associatedText.length >> 24));
                cMac.update((byte)(associatedText.length >> 16));
                cMac.update((byte)(associatedText.length >> 8));
                cMac.update((byte)associatedText.length);
                
                extra = 6;
            }
            
            cMac.update(associatedText, 0, associatedText.length);
            
            extra = (extra + associatedText.length) % 16;
            if (extra != 0)
            {
                for (int i = 0; i != 16 - extra; i++)
                {
                    cMac.update((byte)0x00);
                }
            }
        }
        
        //
        // add the text
        //
        cMac.update(data, dataOff, dataLen);

        return cMac.doFinal(macBlock, 0);
    }

    private boolean hasAssociatedText()
    {
        return associatedText != null && associatedText.length != 0;
    }
}
