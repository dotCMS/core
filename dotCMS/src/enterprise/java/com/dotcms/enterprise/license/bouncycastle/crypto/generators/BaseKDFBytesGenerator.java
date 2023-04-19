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
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ISO18033KDFParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KDFParameters;

/**
 * Basic KDF generator for derived keys and ivs as defined by IEEE P1363a/ISO 18033
 * <br>
 * This implementation is based on ISO 18033/P1363a.
 */
public class BaseKDFBytesGenerator
    implements DerivationFunction
{
    private int     counterStart;
    private Digest  digest;
    private byte[]  shared;
    private byte[]  iv;

    /**
     * Construct a KDF Parameters generator.
     * <p>
     * @param counterStart value of counter.
     * @param digest the digest to be used as the source of derived keys.
     */
    protected BaseKDFBytesGenerator(
        int     counterStart,
        Digest  digest)
    {
        this.counterStart = counterStart;
        this.digest = digest;
    }

    public void init(
        DerivationParameters    param)
    {
        if (param instanceof KDFParameters)
        {
            KDFParameters   p = (KDFParameters)param;

            shared = p.getSharedSecret();
            iv = p.getIV();
        }
        else if (param instanceof ISO18033KDFParameters)
        {
            ISO18033KDFParameters p = (ISO18033KDFParameters)param;
            
            shared = p.getSeed();
            iv = null;
        }
        else
        {
            throw new IllegalArgumentException("KDF parameters required for KDF2Generator");
        }
    }

    /**
     * return the underlying digest.
     */
    public Digest getDigest()
    {
        return digest;
    }

    /**
     * fill len bytes of the output buffer with bytes generated from
     * the derivation function.
     *
     * @throws IllegalArgumentException if the size of the request will cause an overflow.
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

        long    oBytes = len;
        int     outLen = digest.getDigestSize(); 

        //
        // this is at odds with the standard implementation, the
        // maximum value should be hBits * (2^32 - 1) where hBits
        // is the digest output size in bits. We can't have an
        // array with a long index at the moment...
        //
        if (oBytes > ((2L << 32) - 1))
        {
            throw new IllegalArgumentException("Output length too large");
        }

        int cThreshold = (int)((oBytes + outLen - 1) / outLen);

        byte[] dig = null;

        dig = new byte[digest.getDigestSize()];

        int counter = counterStart;
        
        for (int i = 0; i < cThreshold; i++)
        {
            digest.update(shared, 0, shared.length);

            digest.update((byte)(counter >> 24));
            digest.update((byte)(counter >> 16));
            digest.update((byte)(counter >> 8));
            digest.update((byte)counter);
            
            if (iv != null)
            {
                digest.update(iv, 0, iv.length);
            }

            digest.doFinal(dig, 0);

            if (len > outLen)
            {
                System.arraycopy(dig, 0, out, outOff, outLen);
                outOff += outLen;
                len -= outLen;
            }
            else
            {
                System.arraycopy(dig, 0, out, outOff, len);
            }
            
            counter++;
        }
    
        digest.reset();

        return len;
    }
}
