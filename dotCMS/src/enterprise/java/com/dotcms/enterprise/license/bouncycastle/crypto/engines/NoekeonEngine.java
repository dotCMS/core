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
 * A Noekeon engine, using direct-key mode.
 */

public class NoekeonEngine
    implements BlockCipher
{
    private static final int genericSize = 16; // Block and key size, as well as the amount of rounds.
    
    private static final int[] nullVector = 
                               {
                                    0x00, 0x00, 0x00, 0x00 // Used in decryption
                               },
        
                               roundConstants = 
                               {
                                    0x80, 0x1b, 0x36, 0x6c,
                                    0xd8, 0xab, 0x4d, 0x9a,
                                    0x2f, 0x5e, 0xbc, 0x63,
                                    0xc6, 0x97, 0x35, 0x6a,
                                    0xd4
                               };
    
    private int[] state   = new int[4], // a
                  subKeys = new int[4], // k
                  decryptKeys = new int[4];
    
    private boolean _initialised,
                    _forEncryption;
    
    /**
     * Create an instance of the Noekeon encryption algorithm
     * and set some defaults
     */
    public NoekeonEngine()
    {
        _initialised = false;
    }
    
    public String getAlgorithmName()
    {
        return "Noekeon";
    }
    
    public int getBlockSize()
    {
        return genericSize;
    }
    
    /**
     * initialise
     *
     * @param forEncryption whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
                     boolean             forEncryption,
                     CipherParameters    params)
    {
        if (!(params instanceof KeyParameter))
        {
            throw new IllegalArgumentException("invalid parameter passed to Noekeon init - " + params.getClass().getName());
        }
        
        _forEncryption = forEncryption;
        _initialised = true;
        
        KeyParameter       p = (KeyParameter)params;
        
        setKey(p.getKey());
    }
    
    public int processBlock(
                            byte[]  in,
                            int     inOff,
                            byte[]  out,
                            int     outOff)
    {
        if (!_initialised)
        {
            throw new IllegalStateException(getAlgorithmName()+" not initialised");
        }
        
        if ((inOff + genericSize) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }
        
        if ((outOff + genericSize) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }
        
        return (_forEncryption) ? encryptBlock(in, inOff, out, outOff)
                                : decryptBlock(in, inOff, out, outOff);
    }
    
    public void reset()
    {
    }
    
    /**
     * Re-key the cipher.
     * <p>
     * @param  key  the key to be used
     */
    private void setKey(
                        byte[]      key)
    {
        subKeys[0] = bytesToIntBig(key, 0);
        subKeys[1] = bytesToIntBig(key, 4);
        subKeys[2] = bytesToIntBig(key, 8);
        subKeys[3] = bytesToIntBig(key, 12);
    }
    
    private int encryptBlock(
                             byte[]  in,
                             int     inOff,
                             byte[]  out,
                             int     outOff)
    {
        state[0] = bytesToIntBig(in, inOff);
        state[1] = bytesToIntBig(in, inOff+4);
        state[2] = bytesToIntBig(in, inOff+8);
        state[3] = bytesToIntBig(in, inOff+12);
        
        int i;
        for (i = 0; i < genericSize; i++)
        {
            state[0] ^= roundConstants[i];
            theta(state, subKeys);
            pi1(state);
            gamma(state);
            pi2(state);            
        }
        
        state[0] ^= roundConstants[i];
        theta(state, subKeys);
        
        intToBytesBig(state[0], out, outOff);
        intToBytesBig(state[1], out, outOff+4);
        intToBytesBig(state[2], out, outOff+8);
        intToBytesBig(state[3], out, outOff+12);
        
        return genericSize;
    }
    
    private int decryptBlock(
                             byte[]  in,
                             int     inOff,
                             byte[]  out,
                             int     outOff)
    {
        state[0] = bytesToIntBig(in, inOff);
        state[1] = bytesToIntBig(in, inOff+4);
        state[2] = bytesToIntBig(in, inOff+8);
        state[3] = bytesToIntBig(in, inOff+12);
        
        System.arraycopy(subKeys, 0, decryptKeys, 0, subKeys.length);
        theta(decryptKeys, nullVector);
        
        int i;
        for (i = genericSize; i > 0; i--)
        {
            theta(state, decryptKeys);
            state[0] ^= roundConstants[i];
            pi1(state);
            gamma(state);
            pi2(state);
        }
        
        theta(state, decryptKeys);
        state[0] ^= roundConstants[i];
        
        intToBytesBig(state[0], out, outOff);
        intToBytesBig(state[1], out, outOff+4);
        intToBytesBig(state[2], out, outOff+8);
        intToBytesBig(state[3], out, outOff+12);
        
        return genericSize;
    }
        
    private void gamma(int[] a)
    {
        a[1] ^= ~a[3] & ~a[2];
        a[0] ^= a[2] & a[1];
        
        int tmp = a[3];
        a[3]  = a[0];
        a[0]  = tmp;
        a[2] ^= a[0]^a[1]^a[3];
        
        a[1] ^= ~a[3] & ~a[2];
        a[0] ^= a[2] & a[1];
    }
    
    private void theta(int[] a, int[] k)
    {
        int tmp;
        
        tmp   = a[0]^a[2]; 
        tmp  ^= rotl(tmp,8)^rotl(tmp,24); 
        a[1] ^= tmp; 
        a[3] ^= tmp; 
        
        for (int i = 0; i < 4; i++)
        {
            a[i] ^= k[i];
        }
        
        tmp   = a[1]^a[3]; 
        tmp  ^= rotl(tmp,8)^rotl(tmp,24); 
        a[0] ^= tmp; 
        a[2] ^= tmp;
    }
    
    private void pi1(int[] a)
    {
        a[1] = rotl(a[1], 1);
        a[2] = rotl(a[2], 5);
        a[3] = rotl(a[3], 2);
    }
    
    private void pi2(int[] a)
    {
        a[1] = rotl(a[1], 31);
        a[2] = rotl(a[2], 27);
        a[3] = rotl(a[3], 30);
    }
    
    // Helpers
    
    private int bytesToIntBig(byte[] in, int off)
    {
        return ((in[off++]) << 24) |
        ((in[off++] & 0xff) << 16) |
        ((in[off++] & 0xff) <<  8) |
         (in[off  ] & 0xff);
    }
    
    private void intToBytesBig(int x, byte[] out, int off)
    {
        out[off++] = (byte)(x >>> 24);
        out[off++] = (byte)(x >>> 16);
        out[off++] = (byte)(x >>>  8);
        out[off  ] = (byte)x;
    }
    
    private int rotl(int x, int y)
    {
        return (x << y) | (x >>> (32-y));
    }
}
