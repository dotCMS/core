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

import com.dotcms.enterprise.license.bouncycastle.crypto.BasicAgreement;
import com.dotcms.enterprise.license.bouncycastle.crypto.BufferedBlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DerivationFunction;
import com.dotcms.enterprise.license.bouncycastle.crypto.InvalidCipherTextException;
import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.IESParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.IESWithCipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KDFParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

import java.math.BigInteger;

/**
 * support class for constructing intergrated encryption ciphers
 * for doing basic message exchanges on top of key agreement ciphers
 */
public class IESEngine
{
    BasicAgreement      agree;
    DerivationFunction  kdf;
    Mac                 mac;
    BufferedBlockCipher cipher;
    byte[]              macBuf;

    boolean             forEncryption;
    CipherParameters    privParam, pubParam;
    IESParameters       param;

    /**
     * set up for use with stream mode, where the key derivation function
     * is used to provide a stream of bytes to xor with the message.
     *
     * @param agree the key agreement used as the basis for the encryption
     * @param kdf the key derivation function used for byte generation
     * @param mac the message authentication code generator for the message
     */
    public IESEngine(
        BasicAgreement      agree,
        DerivationFunction  kdf,
        Mac                 mac)
    {
        this.agree = agree;
        this.kdf = kdf;
        this.mac = mac;
        this.macBuf = new byte[mac.getMacSize()];
        this.cipher = null;
    }

    /**
     * set up for use in conjunction with a block cipher to handle the
     * message.
     *
     * @param agree the key agreement used as the basis for the encryption
     * @param kdf the key derivation function used for byte generation
     * @param mac the message authentication code generator for the message
     * @param cipher the cipher to used for encrypting the message
     */
    public IESEngine(
        BasicAgreement      agree,
        DerivationFunction  kdf,
        Mac                 mac,
        BufferedBlockCipher cipher)
    {
        this.agree = agree;
        this.kdf = kdf;
        this.mac = mac;
        this.macBuf = new byte[mac.getMacSize()];
        this.cipher = cipher;
    }

    /**
     * Initialise the encryptor.
     *
     * @param forEncryption whether or not this is encryption/decryption.
     * @param privParam our private key parameters
     * @param pubParam the recipient's/sender's public key parameters
     * @param param encoding and derivation parameters.
     */
    public void init(
        boolean                     forEncryption,
        CipherParameters            privParam,
        CipherParameters            pubParam,
        CipherParameters            param)
    {
        this.forEncryption = forEncryption;
        this.privParam = privParam;
        this.pubParam = pubParam;
        this.param = (IESParameters)param;
    }

    private byte[] decryptBlock(
        byte[]  in_enc,
        int     inOff,
        int     inLen,
        byte[]  z)
        throws InvalidCipherTextException
    {
        byte[]          M = null;
        KeyParameter    macKey = null;
        KDFParameters   kParam = new KDFParameters(z, param.getDerivationV());
        int             macKeySize = param.getMacKeySize();

        kdf.init(kParam);

        inLen -= mac.getMacSize();
    
        if (cipher == null)     // stream mode
        {
            byte[] buf = generateKdfBytes(kParam, inLen + (macKeySize / 8));

            M = new byte[inLen];

            for (int i = 0; i != inLen; i++)
            {
                M[i] = (byte)(in_enc[inOff + i] ^ buf[i]);
            }

            macKey = new KeyParameter(buf, inLen, (macKeySize / 8));
        }
        else
        {
            int    cipherKeySize = ((IESWithCipherParameters)param).getCipherKeySize();
            byte[] buf = generateKdfBytes(kParam, (cipherKeySize / 8) + (macKeySize / 8));

            cipher.init(false, new KeyParameter(buf, 0, (cipherKeySize / 8)));

            byte[] tmp = new byte[cipher.getOutputSize(inLen)];

            int len = cipher.processBytes(in_enc, inOff, inLen, tmp, 0);

            len += cipher.doFinal(tmp, len);

            M = new byte[len];

            System.arraycopy(tmp, 0, M, 0, len);

            macKey = new KeyParameter(buf, (cipherKeySize / 8), (macKeySize / 8));
        }

        byte[]  macIV = param.getEncodingV();

        mac.init(macKey);
        mac.update(in_enc, inOff, inLen);
        mac.update(macIV, 0, macIV.length);
        mac.doFinal(macBuf, 0);
    
        inOff += inLen;

        for (int t = 0; t < macBuf.length; t++)
        {           
            if (macBuf[t] != in_enc[inOff + t])
            {
                throw (new InvalidCipherTextException("Mac codes failed to equal."));
            }
        }
       
        return M;
    }

    private byte[] encryptBlock(
        byte[]  in,
        int     inOff,
        int     inLen,
        byte[]  z)
        throws InvalidCipherTextException
    {
        byte[]          C = null;
        KeyParameter    macKey = null;
        KDFParameters   kParam = new KDFParameters(z, param.getDerivationV());
        int             c_text_length = 0;
        int             macKeySize = param.getMacKeySize();

        if (cipher == null)     // stream mode
        {
            byte[] buf = generateKdfBytes(kParam, inLen + (macKeySize / 8));

            C = new byte[inLen + mac.getMacSize()];
            c_text_length = inLen;

            for (int i = 0; i != inLen; i++)
            {
                C[i] = (byte)(in[inOff + i] ^ buf[i]);
            }

            macKey = new KeyParameter(buf, inLen, (macKeySize / 8));
        }
        else
        {
            int    cipherKeySize = ((IESWithCipherParameters)param).getCipherKeySize();
            byte[] buf = generateKdfBytes(kParam, (cipherKeySize / 8) + (macKeySize / 8));

            cipher.init(true, new KeyParameter(buf, 0, (cipherKeySize / 8)));

            c_text_length = cipher.getOutputSize(inLen);

            byte[] tmp = new byte[c_text_length];

            int len = cipher.processBytes(in, inOff, inLen, tmp, 0);

            len += cipher.doFinal(tmp, len);

            C = new byte[len + mac.getMacSize()];
            c_text_length = len;

            System.arraycopy(tmp, 0, C, 0, len);

            macKey = new KeyParameter(buf, (cipherKeySize / 8), (macKeySize / 8));
        }

        byte[]  macIV = param.getEncodingV();

        mac.init(macKey);
        mac.update(C, 0, c_text_length);
        mac.update(macIV, 0, macIV.length);
        //
        // return the message and it's MAC
        //
        mac.doFinal(C, c_text_length);
        return C;
    }

    private byte[] generateKdfBytes(
        KDFParameters kParam,
        int length)
    {
        byte[]  buf = new byte[length];

        kdf.init(kParam);

        kdf.generateBytes(buf, 0, buf.length);

        return buf;
    }

    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        agree.init(privParam);

        BigInteger  z = agree.calculateAgreement(pubParam);

        if (forEncryption)
        {
            return encryptBlock(in, inOff, inLen, z.toByteArray());
        }
        else
        {
            return decryptBlock(in, inOff, inLen, z.toByteArray());
        }
    }
}
