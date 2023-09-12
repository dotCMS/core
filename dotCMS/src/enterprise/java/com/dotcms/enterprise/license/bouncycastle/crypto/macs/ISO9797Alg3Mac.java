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
import com.dotcms.enterprise.license.bouncycastle.crypto.engines.DESEngine;
import com.dotcms.enterprise.license.bouncycastle.crypto.modes.CBCBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.paddings.BlockCipherPadding;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

/**
 * DES based CBC Block Cipher MAC according to ISO9797, algorithm 3 (ANSI X9.19 Retail MAC)
 *
 * This could as well be derived from CBCBlockCipherMac, but then the property mac in the base
 * class must be changed to protected  
 */

public class ISO9797Alg3Mac 
    implements Mac 
{
    private byte[]              mac;
    
    private byte[]              buf;
    private int                 bufOff;
    private BlockCipher         cipher;
    private BlockCipherPadding  padding;
    
    private int                 macSize;
    private KeyParameter        lastKey2;
    private KeyParameter        lastKey3;
    
    /**
     * create a Retail-MAC based on a CBC block cipher. This will produce an
     * authentication code of the length of the block size of the cipher.
     *
     * @param cipher the cipher to be used as the basis of the MAC generation. This must
     * be DESEngine.
     */
    public ISO9797Alg3Mac(
            BlockCipher     cipher)
    {
        this(cipher, cipher.getBlockSize() * 8, null);
    }
    
    /**
     * create a Retail-MAC based on a CBC block cipher. This will produce an
     * authentication code of the length of the block size of the cipher.
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     * @param padding the padding to be used to complete the last block.
     */
    public ISO9797Alg3Mac(
        BlockCipher         cipher,
        BlockCipherPadding  padding)
    {
        this(cipher, cipher.getBlockSize() * 8, padding);
    }

    /**
     * create a Retail-MAC based on a block cipher with the size of the
     * MAC been given in bits. This class uses single DES CBC mode as the basis for the
     * MAC generation.
     * <p>
     * Note: the size of the MAC must be at least 24 bits (FIPS Publication 81),
     * or 16 bits if being used as a data authenticator (FIPS Publication 113),
     * and in general should be less than the size of the block cipher as it reduces
     * the chance of an exhaustive attack (see Handbook of Applied Cryptography).
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     * @param macSizeInBits the size of the MAC in bits, must be a multiple of 8.
     */
    public ISO9797Alg3Mac(
        BlockCipher     cipher,
        int             macSizeInBits)
    {
        this(cipher, macSizeInBits, null);
    }

    /**
     * create a standard MAC based on a block cipher with the size of the
     * MAC been given in bits. This class uses single DES CBC mode as the basis for the
     * MAC generation. The final block is decrypted and then encrypted using the
     * middle and right part of the key.
     * <p>
     * Note: the size of the MAC must be at least 24 bits (FIPS Publication 81),
     * or 16 bits if being used as a data authenticator (FIPS Publication 113),
     * and in general should be less than the size of the block cipher as it reduces
     * the chance of an exhaustive attack (see Handbook of Applied Cryptography).
     *
     * @param cipher the cipher to be used as the basis of the MAC generation.
     * @param macSizeInBits the size of the MAC in bits, must be a multiple of 8.
     * @param padding the padding to be used to complete the last block.
     */
    public ISO9797Alg3Mac(
        BlockCipher         cipher,
        int                 macSizeInBits,
        BlockCipherPadding  padding)
    {
        if ((macSizeInBits % 8) != 0)
        {
            throw new IllegalArgumentException("MAC size must be multiple of 8");
        }

        if (!(cipher instanceof DESEngine))
        {
            throw new IllegalArgumentException("cipher must be instance of DESEngine");
        }

        this.cipher = new CBCBlockCipher(cipher);
        this.padding = padding;
        this.macSize = macSizeInBits / 8;

        mac = new byte[cipher.getBlockSize()];

        buf = new byte[cipher.getBlockSize()];
        bufOff = 0;
    }
    
    public String getAlgorithmName()
    {
        return "ISO9797Alg3";
    }

    public void init(CipherParameters params)
    {
        reset();

        if (!(params instanceof KeyParameter))
        {
            throw new IllegalArgumentException(
                    "params must be an instance of KeyParameter");
        }

        // KeyParameter must contain a double or triple length DES key,
        // however the underlying cipher is a single DES. The middle and
        // right key are used only in the final step.

        KeyParameter kp = (KeyParameter)params;
        KeyParameter key1;
        byte[] keyvalue = kp.getKey();

        if (keyvalue.length == 16)
        { // Double length DES key
            key1 = new KeyParameter(keyvalue, 0, 8);
            this.lastKey2 = new KeyParameter(keyvalue, 8, 8);
            this.lastKey3 = key1;
        }
        else if (keyvalue.length == 24)
        { // Triple length DES key
            key1 = new KeyParameter(keyvalue, 0, 8);
            this.lastKey2 = new KeyParameter(keyvalue, 8, 8);
            this.lastKey3 = new KeyParameter(keyvalue, 16, 8);
        }
        else
        {
            throw new IllegalArgumentException(
                    "Key must be either 112 or 168 bit long");
        }

        cipher.init(true, key1);
    }
    
    public int getMacSize()
    {
        return macSize;
    }
    
    public void update(
            byte        in)
    {
        if (bufOff == buf.length)
        {
            cipher.processBlock(buf, 0, mac, 0);
            bufOff = 0;
        }
        
        buf[bufOff++] = in;
    }
    
    
    public void update(
            byte[]      in,
            int         inOff,
            int         len)
    {
        if (len < 0)
        {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }
        
        int blockSize = cipher.getBlockSize();
        int resultLen = 0;
        int gapLen = blockSize - bufOff;
        
        if (len > gapLen)
        {
            System.arraycopy(in, inOff, buf, bufOff, gapLen);
            
            resultLen += cipher.processBlock(buf, 0, mac, 0);
            
            bufOff = 0;
            len -= gapLen;
            inOff += gapLen;
            
            while (len > blockSize)
            {
                resultLen += cipher.processBlock(in, inOff, mac, 0);
                
                len -= blockSize;
                inOff += blockSize;
            }
        }
        
        System.arraycopy(in, inOff, buf, bufOff, len);
        
        bufOff += len;
    }
    
    public int doFinal(
            byte[]  out,
            int     outOff)
    {
        int blockSize = cipher.getBlockSize();
        
        if (padding == null)
        {
            //
            // pad with zeroes
            //
            while (bufOff < blockSize)
            {
                buf[bufOff] = 0;
                bufOff++;
            }
        }
        else
        {
            if (bufOff == blockSize)
            {
                cipher.processBlock(buf, 0, mac, 0);
                bufOff = 0;
            }
            
            padding.addPadding(buf, bufOff);
        }
        
        cipher.processBlock(buf, 0, mac, 0);

        // Added to code from base class
        DESEngine deseng = new DESEngine();
        
        deseng.init(false, this.lastKey2);
        deseng.processBlock(mac, 0, mac, 0);
        
        deseng.init(true, this.lastKey3);
        deseng.processBlock(mac, 0, mac, 0);
        // ****
        
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
