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
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Wrapper;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

/**
 * an implementation of the AES Key Wrapper from the NIST Key Wrap
 * Specification as described in RFC 3394.
 * <p>
 * For further details see: <a href="http://www.ietf.org/rfc/rfc3394.txt">http://www.ietf.org/rfc/rfc3394.txt</a>
 * and  <a href="http://csrc.nist.gov/encryption/kms/key-wrap.pdf">http://csrc.nist.gov/encryption/kms/key-wrap.pdf</a>.
 */
public class RFC3394WrapEngine
    implements Wrapper
{
    private BlockCipher     engine;
    private KeyParameter    param;
    private boolean         forWrapping;

    private byte[]          iv = {
                              (byte)0xa6, (byte)0xa6, (byte)0xa6, (byte)0xa6,
                              (byte)0xa6, (byte)0xa6, (byte)0xa6, (byte)0xa6 };

    public RFC3394WrapEngine(BlockCipher engine)
    {
        this.engine = engine;
    }

    public void init(
        boolean             forWrapping,
        CipherParameters    param)
    {
        this.forWrapping = forWrapping;

        if (param instanceof ParametersWithRandom)
        {
            param = ((ParametersWithRandom) param).getParameters();
        }

        if (param instanceof KeyParameter)
        {
            this.param = (KeyParameter)param;
        }
        else if (param instanceof ParametersWithIV)
        {
            this.iv = ((ParametersWithIV)param).getIV();
            this.param = (KeyParameter)((ParametersWithIV) param).getParameters();
            if (this.iv.length != 8)
            {
               throw new IllegalArgumentException("IV not equal to 8");
            }
        }
    }

    public String getAlgorithmName()
    {
        return engine.getAlgorithmName();
    }

    public byte[] wrap(
        byte[]  in,
        int     inOff,
        int     inLen)
    {
        if (!forWrapping)
        {
            throw new IllegalStateException("not set for wrapping");
        }

        int     n = inLen / 8;

        if ((n * 8) != inLen)
        {
            throw new DataLengthException("wrap data must be a multiple of 8 bytes");
        }

        byte[]  block = new byte[inLen + iv.length];
        byte[]  buf = new byte[8 + iv.length];

        System.arraycopy(iv, 0, block, 0, iv.length);
        System.arraycopy(in, 0, block, iv.length, inLen);

        engine.init(true, param);

        for (int j = 0; j != 6; j++)
        {
            for (int i = 1; i <= n; i++)
            {
                System.arraycopy(block, 0, buf, 0, iv.length);
                System.arraycopy(block, 8 * i, buf, iv.length, 8);
                engine.processBlock(buf, 0, buf, 0);

                int t = n * j + i;
                for (int k = 1; t != 0; k++)
                {
                    byte    v = (byte)t;

                    buf[iv.length - k] ^= v;

                    t >>>= 8;
                }

                System.arraycopy(buf, 0, block, 0, 8);
                System.arraycopy(buf, 8, block, 8 * i, 8);
            }
        }

        return block;
    }

    public byte[] unwrap(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (forWrapping)
        {
            throw new IllegalStateException("not set for unwrapping");
        }

        int     n = inLen / 8;

        if ((n * 8) != inLen)
        {
            throw new InvalidCipherTextException("unwrap data must be a multiple of 8 bytes");
        }

        byte[]  block = new byte[inLen - iv.length];
        byte[]  a = new byte[iv.length];
        byte[]  buf = new byte[8 + iv.length];

        System.arraycopy(in, 0, a, 0, iv.length);
        System.arraycopy(in, iv.length, block, 0, inLen - iv.length);

        engine.init(false, param);

        n = n - 1;

        for (int j = 5; j >= 0; j--)
        {
            for (int i = n; i >= 1; i--)
            {
                System.arraycopy(a, 0, buf, 0, iv.length);
                System.arraycopy(block, 8 * (i - 1), buf, iv.length, 8);

                int t = n * j + i;
                for (int k = 1; t != 0; k++)
                {
                    byte    v = (byte)t;

                    buf[iv.length - k] ^= v;

                    t >>>= 8;
                }

                engine.processBlock(buf, 0, buf, 0);
                System.arraycopy(buf, 0, a, 0, 8);
                System.arraycopy(buf, 8, block, 8 * (i - 1), 8);
            }
        }

        if (!Arrays.constantTimeAreEqual(a, iv))
        {
            throw new InvalidCipherTextException("checksum failed");
        }

        return block;
    }
}
