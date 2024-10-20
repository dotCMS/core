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

import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.DerivationFunction;
import com.dotcms.enterprise.license.bouncycastle.crypto.DerivationParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.MGFParameters;

/**
 * Generator for MGF1 as defined in PKCS 1v2
 */
public class MGF1BytesGenerator
    implements DerivationFunction
{
    private Digest  digest;
    private byte[]  seed;
    private int     hLen;

    /**
     * @param digest the digest to be used as the source of generated bytes
     */
    public MGF1BytesGenerator(
        Digest  digest)
    {
        this.digest = digest;
        this.hLen = digest.getDigestSize();
    }

    public void init(
        DerivationParameters    param)
    {
        if (!(param instanceof MGFParameters))
        {
            throw new IllegalArgumentException("MGF parameters required for MGF1Generator");
        }

        MGFParameters   p = (MGFParameters)param;

        seed = p.getSeed();
    }

    /**
     * return the underlying digest.
     */
    public Digest getDigest()
    {
        return digest;
    }

    /**
     * int to octet string.
     */
    private void ItoOSP(
        int     i,
        byte[]  sp)
    {
        sp[0] = (byte)(i >>> 24);
        sp[1] = (byte)(i >>> 16);
        sp[2] = (byte)(i >>> 8);
        sp[3] = (byte)(i >>> 0);
    }

    /**
     * fill len bytes of the output buffer with bytes generated from
     * the derivation function.
     *
     * @throws DataLengthException if the out buffer is too small.
     */
    public int generateBytes(
        byte[]  out,
        int     outOff,
        int     len)
        throws DataLengthException, IllegalArgumentException
    {
        if ((out.length - len) < outOff)
        {
            throw new DataLengthException("output buffer too small");
        }
        
        byte[]  hashBuf = new byte[hLen];
        byte[]  C = new byte[4];
        int     counter = 0;

        digest.reset();

        if (len > hLen)
        {
            do
            {
                ItoOSP(counter, C);
    
                digest.update(seed, 0, seed.length);
                digest.update(C, 0, C.length);
                digest.doFinal(hashBuf, 0);
    
                System.arraycopy(hashBuf, 0, out, outOff + counter * hLen, hLen);
            }
            while (++counter < (len / hLen));
        }

        if ((counter * hLen) < len)
        {
            ItoOSP(counter, C);

            digest.update(seed, 0, seed.length);
            digest.update(C, 0, C.length);
            digest.doFinal(hashBuf, 0);

            System.arraycopy(hashBuf, 0, out, outOff + counter * hLen, len - (counter * hLen));
        }

        return len;
    }
}
