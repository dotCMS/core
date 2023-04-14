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
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

public class VMPCEngine implements StreamCipher
{
    /*
     * variables to hold the state of the VMPC engine during encryption and
     * decryption
     */
    protected byte n = 0;
    protected byte[] P = null;
    protected byte s = 0;

    protected byte[] workingIV;
    protected byte[] workingKey;

    public String getAlgorithmName()
    {
        return "VMPC";
    }

    /**
     * initialise a VMPC cipher.
     * 
     * @param forEncryption
     *    whether or not we are for encryption.
     * @param params
     *    the parameters required to set up the cipher.
     * @exception IllegalArgumentException
     *    if the params argument is inappropriate.
     */
    public void init(boolean forEncryption, CipherParameters params)
    {
        if (!(params instanceof ParametersWithIV))
        {
            throw new IllegalArgumentException(
                "VMPC init parameters must include an IV");
        }

        ParametersWithIV ivParams = (ParametersWithIV) params;
        KeyParameter key = (KeyParameter) ivParams.getParameters();

        if (!(ivParams.getParameters() instanceof KeyParameter))
        {
            throw new IllegalArgumentException(
                "VMPC init parameters must include a key");
        }

        this.workingIV = ivParams.getIV();

        if (workingIV == null || workingIV.length < 1 || workingIV.length > 768)
        {
            throw new IllegalArgumentException("VMPC requires 1 to 768 bytes of IV");
        }

        this.workingKey = key.getKey();

        initKey(this.workingKey, this.workingIV);
    }

    protected void initKey(byte[] keyBytes, byte[] ivBytes)
    {
        s = 0;
        P = new byte[256];
        for (int i = 0; i < 256; i++)
        {
            P[i] = (byte) i;
        }

        for (int m = 0; m < 768; m++)
        {
            s = P[(s + P[m & 0xff] + keyBytes[m % keyBytes.length]) & 0xff];
            byte temp = P[m & 0xff];
            P[m & 0xff] = P[s & 0xff];
            P[s & 0xff] = temp;
        }
        for (int m = 0; m < 768; m++)
        {
            s = P[(s + P[m & 0xff] + ivBytes[m % ivBytes.length]) & 0xff];
            byte temp = P[m & 0xff];
            P[m & 0xff] = P[s & 0xff];
            P[s & 0xff] = temp;
        }
        n = 0;
    }

    public void processBytes(byte[] in, int inOff, int len, byte[] out,
        int outOff)
    {
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
            s = P[(s + P[n & 0xff]) & 0xff];
            byte z = P[(P[(P[s & 0xff]) & 0xff] + 1) & 0xff];
            // encryption
            byte temp = P[n & 0xff];
            P[n & 0xff] = P[s & 0xff];
            P[s & 0xff] = temp;
            n = (byte) ((n + 1) & 0xff);

            // xor
            out[i + outOff] = (byte) (in[i + inOff] ^ z);
        }
    }

    public void reset()
    {
        initKey(this.workingKey, this.workingIV);
    }

    public byte returnByte(byte in)
    {
        s = P[(s + P[n & 0xff]) & 0xff];
        byte z = P[(P[(P[s & 0xff]) & 0xff] + 1) & 0xff];
        // encryption
        byte temp = P[n & 0xff];
        P[n & 0xff] = P[s & 0xff];
        P[s & 0xff] = temp;
        n = (byte) ((n + 1) & 0xff);

        // xor
        return (byte) (in ^ z);
    }
}
