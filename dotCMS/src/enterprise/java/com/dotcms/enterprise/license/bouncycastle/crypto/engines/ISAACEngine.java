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

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.StreamCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

/**
 * Implementation of Bob Jenkin's ISAAC (Indirection Shift Accumulate Add and Count).
 * see: http://www.burtleburtle.net/bob/rand/isaacafa.html
*/
public class ISAACEngine
    implements StreamCipher
{
    // Constants
    private final int sizeL          = 8,
                      stateArraySize = sizeL<<5; // 256
    
    // Cipher's internal state
    private int[]   engineState   = null, // mm                
                    results       = null; // randrsl
    private int     a = 0, b = 0, c = 0;
    
    // Engine state
    private int     index         = 0;
    private byte[]  keyStream     = new byte[stateArraySize<<2], // results expanded into bytes
                    workingKey    = null;
    private boolean initialised   = false;
    
    /**
     * initialise an ISAAC cipher.
     *
     * @param forEncryption whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
                     boolean             forEncryption, 
                     CipherParameters     params)
    {
        if (!(params instanceof KeyParameter))
        {
            throw new IllegalArgumentException("invalid parameter passed to ISAAC init - " + params.getClass().getName());
        }
        /* 
         * ISAAC encryption and decryption is completely
         * symmetrical, so the 'forEncryption' is 
         * irrelevant.
         */
        KeyParameter p = (KeyParameter)params;
        setKey(p.getKey());
        
        return;
    }
                    
    public byte returnByte(byte in)
    {
        if (index == 0) 
        {
            isaac();
            keyStream = intToByteLittle(results);
        }
        byte out = (byte)(keyStream[index]^in);
        index = (index + 1) & 1023;
        
        return out;
    }
    
    public void processBytes(
                             byte[]     in, 
                             int     inOff, 
                             int     len, 
                             byte[]     out, 
                             int     outOff)
    {
        if (!initialised)
        {
            throw new IllegalStateException(getAlgorithmName()+" not initialised");
        }
        
        if ((inOff + len) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }
        
        if ((outOff + len) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }
        
        for (int i = 0; i < len; i++)
        {
            if (index == 0) 
            {
                isaac();
                keyStream = intToByteLittle(results);
            }
            out[i+outOff] = (byte)(keyStream[index]^in[i+inOff]);
            index = (index + 1) & 1023;
        }
    }
    
    public String getAlgorithmName()
    {
        return "ISAAC";
    }
    
    public void reset()
    {
        setKey(workingKey);
    }
    
    // Private implementation
    private void setKey(byte[] keyBytes)
    {
        workingKey = keyBytes;
        
        if (engineState == null)
        {
            engineState = new int[stateArraySize];
        }
        
        if (results == null)
        {
            results = new int[stateArraySize];
        }
        
        int i, j, k;
        
        // Reset state
        for (i = 0; i < stateArraySize; i++)
        {
            engineState[i] = results[i] = 0;
        }
        a = b = c = 0;
        
        // Reset index counter for output
        index = 0;
        
        // Convert the key bytes to ints and put them into results[] for initialization
        byte[] t = new byte[keyBytes.length + (keyBytes.length & 3)];
        System.arraycopy(keyBytes, 0, t, 0, keyBytes.length);
        for (i = 0; i < t.length; i+=4)
        {
            results[i>>2] = byteToIntLittle(t, i);
        }
        
        // It has begun?
        int[] abcdefgh = new int[sizeL];
        
        for (i = 0; i < sizeL; i++)
        {
            abcdefgh[i] = 0x9e3779b9; // Phi (golden ratio)
        }
        
        for (i = 0; i < 4; i++)
        {
            mix(abcdefgh);
        }
        
        for (i = 0; i < 2; i++)
        {
            for (j = 0; j < stateArraySize; j+=sizeL)
            {
                for (k = 0; k < sizeL; k++)
                {
                    abcdefgh[k] += (i<1) ? results[j+k] : engineState[j+k];
                }
                
                mix(abcdefgh);
                
                for (k = 0; k < sizeL; k++)
                {
                    engineState[j+k] = abcdefgh[k];
                }
            }
        }
        
        isaac();
        
        initialised = true;
    }    
    
    private void isaac()
    {
        int i, x, y;
        
        b += ++c;
        for (i = 0; i < stateArraySize; i++)
        {
            x = engineState[i];
            switch (i & 3)
            {
                case 0: a ^= (a <<  13); break;
                case 1: a ^= (a >>>  6); break;
                case 2: a ^= (a <<   2); break;
                case 3: a ^= (a >>> 16); break;
            }
            a += engineState[(i+128) & 0xFF];
            engineState[i] = y = engineState[(x >>> 2) & 0xFF] + a + b;
            results[i] = b = engineState[(y >>> 10) & 0xFF] + x;
        }
    }
    
    private void mix(int[] x)
    {
        x[0]^=x[1]<< 11; x[3]+=x[0]; x[1]+=x[2];
        x[1]^=x[2]>>> 2; x[4]+=x[1]; x[2]+=x[3];
        x[2]^=x[3]<<  8; x[5]+=x[2]; x[3]+=x[4];
        x[3]^=x[4]>>>16; x[6]+=x[3]; x[4]+=x[5];
        x[4]^=x[5]<< 10; x[7]+=x[4]; x[5]+=x[6];
        x[5]^=x[6]>>> 4; x[0]+=x[5]; x[6]+=x[7];
        x[6]^=x[7]<<  8; x[1]+=x[6]; x[7]+=x[0];
        x[7]^=x[0]>>> 9; x[2]+=x[7]; x[0]+=x[1];
    }
    
    private int byteToIntLittle(byte[] x, int offset)
    {
        return (int)(x[offset++] & 0xFF)        |
                   ((x[offset++] & 0xFF) <<  8) |
                   ((x[offset++] & 0xFF) << 16) |
                    (x[offset++] << 24);
    }
    
    private byte[] intToByteLittle(int x)
    {
        byte[] out = new byte[4];
        out[3] = (byte)x;
        out[2] = (byte)(x >>> 8);
        out[1] = (byte)(x >>> 16);
        out[0] = (byte)(x >>> 24);
        return out;
    } 
    
    private byte[] intToByteLittle(int[] x)
    {
        byte[] out = new byte[4*x.length];
        for (int i = 0, j = 0; i < x.length; i++,j+=4)
        {
            System.arraycopy(intToByteLittle(x[i]), 0, out, j, 4);
        }
        return out;
    }
}
