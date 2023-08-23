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

package com.dotcms.enterprise.license.bouncycastle.crypto.encodings;

import com.dotcms.enterprise.license.bouncycastle.crypto.AsymmetricBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithRandom;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;

/**
 * this does your basic PKCS 1 v1.5 padding - whether or not you should be using this
 * depends on your application - see PKCS1 Version 2 for details.
 */
public class PKCS1Encoding
    implements AsymmetricBlockCipher
{
    /**
     * some providers fail to include the leading zero in PKCS1 encoded blocks. If you need to
     * work with one of these set the system property org.bouncycastle.pkcs1.strict to false.
     * <p>
     * The system property is checked during construction of the encoding object, it is set to 
     * true by default.
     * </p>
     */
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "org.bouncycastle.pkcs1.strict";
    
    private static final int HEADER_LENGTH = 10;

    private SecureRandom            random;
    private AsymmetricBlockCipher   engine;
    private boolean                 forEncryption;
    private boolean                 forPrivateKey;
    private boolean                 useStrictLength;

    /**
     * Basic constructor.
     * @param cipher
     */
    public PKCS1Encoding(
        AsymmetricBlockCipher   cipher)
    {
        this.engine = cipher;
        this.useStrictLength = useStrict();
    }   

    //
    // for J2ME compatibility
    //
    private boolean useStrict()
    {
        // required if security manager has been installed.
        String strict = (String)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return System.getProperty(STRICT_LENGTH_ENABLED_PROPERTY);
            }
        });

        return strict == null || strict.equals("true");
    }

    public AsymmetricBlockCipher getUnderlyingCipher()
    {
        return engine;
    }

    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        AsymmetricKeyParameter  kParam;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter)rParam.getParameters();
        }
        else
        {
            this.random = new SecureRandom();
            kParam = (AsymmetricKeyParameter)param;
        }

        engine.init(forEncryption, param);

        this.forPrivateKey = kParam.isPrivate();
        this.forEncryption = forEncryption;
    }

    public int getInputBlockSize()
    {
        int     baseBlockSize = engine.getInputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize - HEADER_LENGTH;
        }
        else
        {
            return baseBlockSize;
        }
    }

    public int getOutputBlockSize()
    {
        int     baseBlockSize = engine.getOutputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize;
        }
        else
        {
            return baseBlockSize - HEADER_LENGTH;
        }
    }

    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (forEncryption)
        {
            return encodeBlock(in, inOff, inLen);
        }
        else
        {
            return decodeBlock(in, inOff, inLen);
        }
    }

    private byte[] encodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (inLen > getInputBlockSize())
        {
            throw new IllegalArgumentException("input data too large");
        }
        
        byte[]  block = new byte[engine.getInputBlockSize()];

        if (forPrivateKey)
        {
            block[0] = 0x01;                        // type code 1

            for (int i = 1; i != block.length - inLen - 1; i++)
            {
                block[i] = (byte)0xFF;
            }
        }
        else
        {
            random.nextBytes(block);                // random fill

            block[0] = 0x02;                        // type code 2

            //
            // a zero byte marks the end of the padding, so all
            // the pad bytes must be non-zero.
            //
            for (int i = 1; i != block.length - inLen - 1; i++)
            {
                while (block[i] == 0)
                {
                    block[i] = (byte)random.nextInt();
                }
            }
        }

        block[block.length - inLen - 1] = 0x00;       // mark the end of the padding
        System.arraycopy(in, inOff, block, block.length - inLen, inLen);

        return engine.processBlock(block, 0, block.length);
    }

    /**
     * @exception InvalidCipherTextException if the decrypted block is not in PKCS1 format.
     */
    private byte[] decodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        byte[]  block = engine.processBlock(in, inOff, inLen);

        if (block.length < getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block truncated");
        }

        byte type = block[0];
        
        if (type != 1 && type != 2)
        {
            throw new InvalidCipherTextException("unknown block type");
        }

        if (useStrictLength && block.length != engine.getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block incorrect size");
        }
        
        //
        // find and extract the message block.
        //
        int start;
        
        for (start = 1; start != block.length; start++)
        {
            byte pad = block[start];
            
            if (pad == 0)
            {
                break;
            }
            if (type == 1 && pad != (byte)0xff)
            {
                throw new InvalidCipherTextException("block padding incorrect");
            }
        }

        start++;           // data should start at the next byte

        if (start > block.length || start < HEADER_LENGTH)
        {
            throw new InvalidCipherTextException("no data in block");
        }

        byte[]  result = new byte[block.length - start];

        System.arraycopy(block, start, result, 0, result.length);

        return result;
    }
}
