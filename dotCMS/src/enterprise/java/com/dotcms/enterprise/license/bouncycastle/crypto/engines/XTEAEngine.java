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
 * An XTEA engine.
 */
public class XTEAEngine
    implements BlockCipher
{
    private static final int rounds     = 32,
                             block_size = 8,
//                             key_size   = 16,
                             delta      = 0x9E3779B9;

    /*
     * the expanded key array of 4 subkeys
     */
    private int[]   _S    = new int[4],
                    _sum0 = new int[32],
                    _sum1 = new int[32];
    private boolean _initialised,
                    _forEncryption;

    /**
     * Create an instance of the TEA encryption algorithm
     * and set some defaults
     */
    public XTEAEngine()
    {
        _initialised = false;
    }

    public String getAlgorithmName()
    {
        return "XTEA";
    }

    public int getBlockSize()
    {
        return block_size;
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
            throw new IllegalArgumentException("invalid parameter passed to TEA init - " + params.getClass().getName());
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

        if ((inOff + block_size) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if ((outOff + block_size) > out.length)
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
        int i, j;
        for (i = j = 0; i < 4; i++,j+=4)
        {
            _S[i] = bytesToInt(key, j);
        }
            
        for (i = j = 0; i < rounds; i++)
        {
                _sum0[i] = (j + _S[j & 3]);
                j += delta;
                _sum1[i] = (j + _S[j >>> 11 & 3]);
        }
    }

    private int encryptBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        // Pack bytes into integers
        int v0 = bytesToInt(in, inOff);
        int v1 = bytesToInt(in, inOff + 4);

        for (int i = 0; i < rounds; i++)
        {
            v0    += ((v1 << 4 ^ v1 >>> 5) + v1) ^ _sum0[i];
            v1    += ((v0 << 4 ^ v0 >>> 5) + v0) ^ _sum1[i];
        }

        unpackInt(v0, out, outOff);
        unpackInt(v1, out, outOff + 4);

        return block_size;
    }

    private int decryptBlock(
        byte[]  in,
        int     inOff,
        byte[]  out,
        int     outOff)
    {
        // Pack bytes into integers
        int v0 = bytesToInt(in, inOff);
        int v1 = bytesToInt(in, inOff + 4);

        for (int i = rounds-1; i >= 0; i--)
        {
            v1  -= ((v0 << 4 ^ v0 >>> 5) + v0) ^ _sum1[i];
            v0  -= ((v1 << 4 ^ v1 >>> 5) + v1) ^ _sum0[i];
        }

        unpackInt(v0, out, outOff);
        unpackInt(v1, out, outOff + 4);

        return block_size;
    }

    private int bytesToInt(byte[] in, int inOff)
    {
        return ((in[inOff++]) << 24) |
                 ((in[inOff++] & 255) << 16) |
                 ((in[inOff++] & 255) <<  8) |
                 ((in[inOff] & 255));
    }

    private void unpackInt(int v, byte[] out, int outOff)
    {
        out[outOff++] = (byte)(v >>> 24);
        out[outOff++] = (byte)(v >>> 16);
        out[outOff++] = (byte)(v >>>  8);
        out[outOff  ] = (byte)v;
    }
}
