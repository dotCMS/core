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

public class RC4Engine implements StreamCipher
{
    private final static int STATE_LENGTH = 256;

    /*
     * variables to hold the state of the RC4 engine
     * during encryption and decryption
     */

    private byte[]      engineState = null;
    private int         x = 0;
    private int         y = 0;
    private byte[]      workingKey = null;

    /**
     * initialise a RC4 cipher.
     *
     * @param forEncryption whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean             forEncryption, 
        CipherParameters     params
   )
    {
        if (params instanceof KeyParameter)
        {
            /* 
             * RC4 encryption and decryption is completely
             * symmetrical, so the 'forEncryption' is 
             * irrelevant.
             */
            workingKey = ((KeyParameter)params).getKey();
            setKey(workingKey);

            return;
        }

        throw new IllegalArgumentException("invalid parameter passed to RC4 init - " + params.getClass().getName());
    }

    public String getAlgorithmName()
    {
        return "RC4";
    }

    public byte returnByte(byte in)
    {
        x = (x + 1) & 0xff;
        y = (engineState[x] + y) & 0xff;

        // swap
        byte tmp = engineState[x];
        engineState[x] = engineState[y];
        engineState[y] = tmp;

        // xor
        return (byte)(in ^ engineState[(engineState[x] + engineState[y]) & 0xff]);
    }

    public void processBytes(
        byte[]     in, 
        int     inOff, 
        int     len, 
        byte[]     out, 
        int     outOff)
    {
        if ((inOff + len) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if ((outOff + len) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }

        for (int i = 0; i < len ; i++)
        {
            x = (x + 1) & 0xff;
            y = (engineState[x] + y) & 0xff;

            // swap
            byte tmp = engineState[x];
            engineState[x] = engineState[y];
            engineState[y] = tmp;

            // xor
            out[i+outOff] = (byte)(in[i + inOff]
                    ^ engineState[(engineState[x] + engineState[y]) & 0xff]);
        }
    }

    public void reset()
    {
        setKey(workingKey);
    }

    // Private implementation

    private void setKey(byte[] keyBytes)
    {
        workingKey = keyBytes;

        // System.out.println("the key length is ; "+ workingKey.length);

        x = 0;
        y = 0;

        if (engineState == null)
        {
            engineState = new byte[STATE_LENGTH];
        }

        // reset the state of the engine
        for (int i=0; i < STATE_LENGTH; i++)
        {
            engineState[i] = (byte)i;
        }
        
        int i1 = 0;
        int i2 = 0;

        for (int i=0; i < STATE_LENGTH; i++)
        {
            i2 = ((keyBytes[i1] & 0xff) + engineState[i] + i2) & 0xff;
            // do the byte-swap inline
            byte tmp = engineState[i];
            engineState[i] = engineState[i2];
            engineState[i2] = tmp;
            i1 = (i1+1) % keyBytes.length; 
        }
    }
}
