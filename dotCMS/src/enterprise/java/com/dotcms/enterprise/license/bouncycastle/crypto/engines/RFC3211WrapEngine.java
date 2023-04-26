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
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Wrapper;
import com.dotcms.enterprise.license.bouncycastle.crypto.modes.CBCBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;

import java.security.SecureRandom;

/**
 * an implementation of the RFC 3211 Key Wrap
 * Specification.
 */
public class RFC3211WrapEngine
    implements Wrapper
{
    private CBCBlockCipher   engine;
    private ParametersWithIV param;
    private boolean          forWrapping;
    private SecureRandom     rand;

    public RFC3211WrapEngine(BlockCipher engine)
    {
        this.engine = new CBCBlockCipher(engine);
    }

    public void init(
        boolean          forWrapping,
        CipherParameters param)
    {
        this.forWrapping = forWrapping;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom p = (ParametersWithRandom)param;

            rand = p.getRandom();
            this.param = (ParametersWithIV)p.getParameters();
        }
        else
        {
            if (forWrapping)
            {
                rand = new SecureRandom();
            }

            this.param = (ParametersWithIV)param;
        }
    }

    public String getAlgorithmName()
    {
        return engine.getUnderlyingCipher().getAlgorithmName() + "/RFC3211Wrap";
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

        engine.init(true, param);

        int blockSize = engine.getBlockSize();
        byte[] cekBlock;

        if (inLen + 4 < blockSize * 2)
        {
            cekBlock = new byte[blockSize * 2];
        }
        else
        {
            cekBlock = new byte[(inLen + 4) % blockSize == 0 ? inLen + 4 : ((inLen + 4) / blockSize + 1) * blockSize];
        }

        cekBlock[0] = (byte)inLen;
        cekBlock[1] = (byte)~in[inOff];
        cekBlock[2] = (byte)~in[inOff + 1];
        cekBlock[3] = (byte)~in[inOff + 2];

        System.arraycopy(in, inOff, cekBlock, 4, inLen);

        for (int i = inLen + 4; i < cekBlock.length; i++)
        {
            cekBlock[i] = (byte)rand.nextInt();
        }

        for (int i = 0; i < cekBlock.length; i += blockSize)
        {
            engine.processBlock(cekBlock, i, cekBlock, i);
        }

        for (int i = 0; i < cekBlock.length; i += blockSize)
        {
            engine.processBlock(cekBlock, i, cekBlock, i);
        }

        return cekBlock;
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

        int blockSize = engine.getBlockSize();

        if (inLen < 2 * blockSize)
        {
            throw new InvalidCipherTextException("input too short");
        }
        
        byte[] cekBlock = new byte[inLen];
        byte[] iv = new byte[blockSize];

        System.arraycopy(in, inOff, cekBlock, 0, inLen);
        System.arraycopy(in, inOff, iv, 0, iv.length);
        
        engine.init(false, new ParametersWithIV(param.getParameters(), iv));

        for (int i = blockSize; i < cekBlock.length; i += blockSize)
        {
            engine.processBlock(cekBlock, i, cekBlock, i);    
        }

        System.arraycopy(cekBlock, cekBlock.length - iv.length, iv, 0, iv.length);

        engine.init(false, new ParametersWithIV(param.getParameters(), iv));

        engine.processBlock(cekBlock, 0, cekBlock, 0);

        engine.init(false, param);

        for (int i = 0; i < cekBlock.length; i += blockSize)
        {
            engine.processBlock(cekBlock, i, cekBlock, i);
        }

        if ((cekBlock[0] & 0xff) > cekBlock.length - 4)
        {
            throw new InvalidCipherTextException("wrapped key corrupted");
        }

        byte[] key = new byte[cekBlock[0] & 0xff];

        System.arraycopy(cekBlock, 4, key, 0, cekBlock[0]);

        // Note: Using constant time comparison
        int nonEqual = 0;
        for (int i = 0; i != 3; i++)
        {
            byte check = (byte)~cekBlock[1 + i];
            nonEqual |= (check ^ key[i]);
        }
        if (nonEqual != 0)
        {
            throw new InvalidCipherTextException("wrapped key fails checksum");
        }

        return key;
    }
}
