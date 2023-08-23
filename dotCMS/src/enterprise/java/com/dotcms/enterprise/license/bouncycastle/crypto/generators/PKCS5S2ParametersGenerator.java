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

package com.dotcms.enterprise.license.bouncycastle.crypto.generators;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.PBEParametersGenerator;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.macs.HMac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Generator for PBE derived keys and ivs as defined by PKCS 5 V2.0 Scheme 2.
 * This generator uses a SHA-1 HMac as the calculation function.
 * <p>
 * The document this implementation is based on can be found at
 * <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html>
 * RSA's PKCS5 Page</a>
 */
public class PKCS5S2ParametersGenerator
    extends PBEParametersGenerator
{
    private Mac    hMac = new HMac(new SHA1Digest());

    /**
     * construct a PKCS5 Scheme 2 Parameters generator.
     */
    public PKCS5S2ParametersGenerator()
    {
    }

    private void F(
        byte[]  P,
        byte[]  S,
        int     c,
        byte[]  iBuf,
        byte[]  out,
        int     outOff)
    {
        byte[]              state = new byte[hMac.getMacSize()];
        CipherParameters    param = new KeyParameter(P);

        hMac.init(param);

        if (S != null)
        {
            hMac.update(S, 0, S.length);
        }

        hMac.update(iBuf, 0, iBuf.length);

        hMac.doFinal(state, 0);

        System.arraycopy(state, 0, out, outOff, state.length);

        if (c == 0)
        {
            throw new IllegalArgumentException("iteration count must be at least 1.");
        }
        
        for (int count = 1; count < c; count++)
        {
            hMac.init(param);
            hMac.update(state, 0, state.length);
            hMac.doFinal(state, 0);

            for (int j = 0; j != state.length; j++)
            {
                out[outOff + j] ^= state[j];
            }
        }
    }

    private void intToOctet(
        byte[]  buf,
        int     i)
    {
        buf[0] = (byte)(i >>> 24);
        buf[1] = (byte)(i >>> 16);
        buf[2] = (byte)(i >>> 8);
        buf[3] = (byte)i;
    }

    private byte[] generateDerivedKey(
        int dkLen)
    {
        int     hLen = hMac.getMacSize();
        int     l = (dkLen + hLen - 1) / hLen;
        byte[]  iBuf = new byte[4];
        byte[]  out = new byte[l * hLen];

        for (int i = 1; i <= l; i++)
        {
            intToOctet(iBuf, i);

            F(password, salt, iterationCount, iBuf, out, (i - 1) * hLen);
        }

        return out;
    }

    /**
     * Generate a key parameter derived from the password, salt, and iteration
     * count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     */
    public CipherParameters generateDerivedParameters(
        int keySize)
    {
        keySize = keySize / 8;

        byte[]  dKey = generateDerivedKey(keySize);

        return new KeyParameter(dKey, 0, keySize);
    }

    /**
     * Generate a key with initialisation vector parameter derived from
     * the password, salt, and iteration count we are currently initialised
     * with.
     *
     * @param keySize the size of the key we want (in bits)
     * @param ivSize the size of the iv we want (in bits)
     * @return a ParametersWithIV object.
     */
    public CipherParameters generateDerivedParameters(
        int     keySize,
        int     ivSize)
    {
        keySize = keySize / 8;
        ivSize = ivSize / 8;

        byte[]  dKey = generateDerivedKey(keySize + ivSize);

        return new ParametersWithIV(new KeyParameter(dKey, 0, keySize), dKey, keySize, ivSize);
    }

    /**
     * Generate a key parameter for use with a MAC derived from the password,
     * salt, and iteration count we are currently initialised with.
     *
     * @param keySize the size of the key we want (in bits)
     * @return a KeyParameter object.
     */
    public CipherParameters generateDerivedMacParameters(
        int keySize)
    {
        return generateDerivedParameters(keySize);
    }
}
