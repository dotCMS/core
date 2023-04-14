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

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

public class VMPCMac implements Mac
{
    private byte g;

    private byte n = 0;
    private byte[] P = null;
    private byte s = 0;

    private byte[] T;
    private byte[] workingIV;

    private byte[] workingKey;

    private byte x1, x2, x3, x4;

    public int doFinal(byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {
        // Execute the Post-Processing Phase
        for (int r = 1; r < 25; r++)
        {
            s = P[(s + P[n & 0xff]) & 0xff];

            x4 = P[(x4 + x3 + r) & 0xff];
            x3 = P[(x3 + x2 + r) & 0xff];
            x2 = P[(x2 + x1 + r) & 0xff];
            x1 = P[(x1 + s + r) & 0xff];
            T[g & 0x1f] = (byte) (T[g & 0x1f] ^ x1);
            T[(g + 1) & 0x1f] = (byte) (T[(g + 1) & 0x1f] ^ x2);
            T[(g + 2) & 0x1f] = (byte) (T[(g + 2) & 0x1f] ^ x3);
            T[(g + 3) & 0x1f] = (byte) (T[(g + 3) & 0x1f] ^ x4);
            g = (byte) ((g + 4) & 0x1f);

            byte temp = P[n & 0xff];
            P[n & 0xff] = P[s & 0xff];
            P[s & 0xff] = temp;
            n = (byte) ((n + 1) & 0xff);
        }

        // Input T to the IV-phase of the VMPC KSA
        for (int m = 0; m < 768; m++)
        {
            s = P[(s + P[m & 0xff] + T[m & 0x1f]) & 0xff];
            byte temp = P[m & 0xff];
            P[m & 0xff] = P[s & 0xff];
            P[s & 0xff] = temp;
        }

        // Store 20 new outputs of the VMPC Stream Cipher in table M
        byte[] M = new byte[20];
        for (int i = 0; i < 20; i++)
        {
            s = P[(s + P[i & 0xff]) & 0xff];
            M[i] = P[(P[(P[s & 0xff]) & 0xff] + 1) & 0xff];

            byte temp = P[i & 0xff];
            P[i & 0xff] = P[s & 0xff];
            P[s & 0xff] = temp;
        }

        System.arraycopy(M, 0, out, outOff, M.length);
        reset();

        return M.length;
    }

    public String getAlgorithmName()
    {
        return "VMPC-MAC";
    }

    public int getMacSize()
    {
        return 20;
    }

    public void init(CipherParameters params) throws IllegalArgumentException
    {
        if (!(params instanceof ParametersWithIV))
        {
            throw new IllegalArgumentException(
                "VMPC-MAC Init parameters must include an IV");
        }

        ParametersWithIV ivParams = (ParametersWithIV) params;
        KeyParameter key = (KeyParameter) ivParams.getParameters();

        if (!(ivParams.getParameters() instanceof KeyParameter))
        {
            throw new IllegalArgumentException(
                "VMPC-MAC Init parameters must include a key");
        }

        this.workingIV = ivParams.getIV();

        if (workingIV == null || workingIV.length < 1 || workingIV.length > 768)
        {
            throw new IllegalArgumentException(
                "VMPC-MAC requires 1 to 768 bytes of IV");
        }

        this.workingKey = key.getKey();

        reset();

    }

    private void initKey(byte[] keyBytes, byte[] ivBytes)
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

    public void reset()
    {
        initKey(this.workingKey, this.workingIV);
        g = x1 = x2 = x3 = x4 = n = 0;
        T = new byte[32];
        for (int i = 0; i < 32; i++)
        {
            T[i] = 0;
        }
    }

    public void update(byte in) throws IllegalStateException
    {
        s = P[(s + P[n & 0xff]) & 0xff];
        byte c = (byte) (in ^ P[(P[(P[s & 0xff]) & 0xff] + 1) & 0xff]);

        x4 = P[(x4 + x3) & 0xff];
        x3 = P[(x3 + x2) & 0xff];
        x2 = P[(x2 + x1) & 0xff];
        x1 = P[(x1 + s + c) & 0xff];
        T[g & 0x1f] = (byte) (T[g & 0x1f] ^ x1);
        T[(g + 1) & 0x1f] = (byte) (T[(g + 1) & 0x1f] ^ x2);
        T[(g + 2) & 0x1f] = (byte) (T[(g + 2) & 0x1f] ^ x3);
        T[(g + 3) & 0x1f] = (byte) (T[(g + 3) & 0x1f] ^ x4);
        g = (byte) ((g + 4) & 0x1f);

        byte temp = P[n & 0xff];
        P[n & 0xff] = P[s & 0xff];
        P[s & 0xff] = temp;
        n = (byte) ((n + 1) & 0xff);
    }

    public void update(byte[] in, int inOff, int len)
        throws DataLengthException, IllegalStateException
    {
        if ((inOff + len) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        for (int i = 0; i < len; i++)
        {
            update(in[i]);
        }
    }
}
