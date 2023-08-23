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

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

import java.io.IOException;

/**
 * A generic TLS 1.0 block cipher suite. This can be used for AES or 3DES for
 * example.
 */
public class TlsBlockCipherCipherSuite extends TlsCipherSuite
{

    private BlockCipher encryptCipher;

    private BlockCipher decryptCipher;

    private Digest writeDigest;

    private Digest readDigest;

    private int cipherKeySize;

    private short keyExchange;

    private TlsMac writeMac;

    private TlsMac readMac;

    protected TlsBlockCipherCipherSuite(BlockCipher encrypt,
                                        BlockCipher decrypt, Digest writeDigest, Digest readDigest,
                                        int cipherKeySize, short keyExchange)
    {
        this.encryptCipher = encrypt;
        this.decryptCipher = decrypt;
        this.writeDigest = writeDigest;
        this.readDigest = readDigest;
        this.cipherKeySize = cipherKeySize;
        this.keyExchange = keyExchange;
    }

    protected void init(byte[] ms, byte[] cr, byte[] sr)
    {
        int prfSize = (2 * cipherKeySize) + (2 * writeDigest.getDigestSize())
            + (2 * encryptCipher.getBlockSize());
        byte[] key_block = new byte[prfSize];
        byte[] random = new byte[cr.length + sr.length];
        System.arraycopy(cr, 0, random, sr.length, cr.length);
        System.arraycopy(sr, 0, random, 0, sr.length);
        TlsUtils.PRF(ms, TlsUtils.toByteArray("key expansion"), random, key_block);

        int offset = 0;

        // Init MACs
        writeMac = new TlsMac(writeDigest, key_block, offset, writeDigest
            .getDigestSize());
        offset += writeDigest.getDigestSize();
        readMac = new TlsMac(readDigest, key_block, offset, readDigest
            .getDigestSize());
        offset += readDigest.getDigestSize();

        // Init Ciphers
        this.initCipher(true, encryptCipher, key_block, cipherKeySize, offset,
            offset + (cipherKeySize * 2));
        offset += cipherKeySize;
        this.initCipher(false, decryptCipher, key_block, cipherKeySize, offset,
            offset + cipherKeySize + decryptCipher.getBlockSize());
    }

    private void initCipher(boolean forEncryption, BlockCipher cipher,
                            byte[] key_block, int key_size, int key_offset, int iv_offset)
    {
        KeyParameter key_parameter = new KeyParameter(key_block, key_offset,
            key_size);
        ParametersWithIV parameters_with_iv = new ParametersWithIV(
            key_parameter, key_block, iv_offset, cipher.getBlockSize());
        cipher.init(forEncryption, parameters_with_iv);
    }

    protected byte[] encodePlaintext(short type, byte[] plaintext, int offset,
                                     int len)
    {
        int blocksize = encryptCipher.getBlockSize();
        int paddingsize = blocksize
            - ((len + writeMac.getSize() + 1) % blocksize);
        int totalsize = len + writeMac.getSize() + paddingsize + 1;
        byte[] outbuf = new byte[totalsize];
        System.arraycopy(plaintext, offset, outbuf, 0, len);
        byte[] mac = writeMac.calculateMac(type, plaintext, offset, len);
        System.arraycopy(mac, 0, outbuf, len, mac.length);
        int paddoffset = len + mac.length;
        for (int i = 0; i <= paddingsize; i++)
        {
            outbuf[i + paddoffset] = (byte)paddingsize;
        }
        for (int i = 0; i < totalsize; i += blocksize)
        {
            encryptCipher.processBlock(outbuf, i, outbuf, i);
        }
        return outbuf;

    }

    protected byte[] decodeCiphertext(short type, byte[] ciphertext,
                                      int offset, int len, TlsProtocolHandler handler) throws IOException
    {
        int blocksize = decryptCipher.getBlockSize();
        boolean decrypterror = false;

        /*
        * Decrypt all the ciphertext using the blockcipher
        */
        for (int i = 0; i < len; i += blocksize)
        {
            decryptCipher.processBlock(ciphertext, i + offset, ciphertext, i
                + offset);
        }

        /*
        * Check if padding is correct
        */
        int paddingsize = ciphertext[offset + len - 1];
        if (offset + len - 1 - paddingsize < 0)
        {
            /*
             * This would lead to a negative array index, so this padding
             * must be incorrect!
             */
            decrypterror = true;
            paddingsize = 0;
        }
        else
        {
            /*
             * Now, check all the padding-bytes.
             */
            for (int i = 0; i <= paddingsize; i++)
            {
                if (ciphertext[offset + len - 1 - i] != paddingsize)
                {
                    /* Wrong padding */
                    decrypterror = true;
                }
            }
        }

        /*
        * We now don't care if padding verification has failed or not,
        * we will calculate the mac to give an attacker no kind of timing
        * profile he can use to find out if mac verification failed or
        * padding verification failed.
        */
        int plaintextlength = len - readMac.getSize() - paddingsize - 1;
        byte[] calculatedMac = readMac.calculateMac(type, ciphertext, offset,
            plaintextlength);

        /*
        * Check all bytes in the mac.
        */
        for (int i = 0; i < calculatedMac.length; i++)
        {
            if (ciphertext[offset + plaintextlength + i] != calculatedMac[i])
            {
                decrypterror = true;
            }
        }

        /*
        * Now, it is safe to fail.
        */
        if (decrypterror)
        {
            handler.failWithError(TlsProtocolHandler.AL_fatal,
                TlsProtocolHandler.AP_bad_record_mac);
        }
        byte[] plaintext = new byte[plaintextlength];
        System.arraycopy(ciphertext, offset, plaintext, 0, plaintextlength);
        return plaintext;

    }

    protected short getKeyExchangeAlgorithm()
    {
        return this.keyExchange;
    }

}
