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

import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.macs.HMac;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A generic TLS MAC implementation, which can be used with any kind of
 * Digest to act as an HMAC.
 */
public class TlsMac
{
    private long seqNo;
    private HMac mac;

    /**
     * Generate a new instance of an TlsMac.
     *
     * @param digest    The digest to use.
     * @param key_block A byte-array where the key for this mac is located.
     * @param offset    The number of bytes to skip, before the key starts in the buffer.
     * @param len       The length of the key.
     */
    protected TlsMac(Digest digest, byte[] key_block, int offset, int len)
    {
        this.mac = new HMac(digest);
        KeyParameter param = new KeyParameter(key_block, offset, len);
        this.mac.init(param);
        this.seqNo = 0;
    }

    /**
     * @return The Keysize of the mac.
     */
    protected int getSize()
    {
        return mac.getMacSize();
    }

    /**
     * Calculate the mac for some given data.
     * <p/>
     * TlsMac will keep track of the sequence number internally.
     *
     * @param type    The message type of the message.
     * @param message A byte-buffer containing the message.
     * @param offset  The number of bytes to skip, before the message starts.
     * @param len     The length of the message.
     * @return A new byte-buffer containing the mac value.
     */
    protected byte[] calculateMac(short type, byte[] message, int offset, int len)
    {
        try
        {
            ByteArrayOutputStream bosMac = new ByteArrayOutputStream();
            TlsUtils.writeUint64(seqNo++, bosMac);
            TlsUtils.writeUint8(type, bosMac);
            TlsUtils.writeVersion(bosMac);
            TlsUtils.writeUint16(len, bosMac);
            bosMac.write(message, offset, len);
            byte[] macData = bosMac.toByteArray();
            mac.update(macData, 0, macData.length);
            byte[] result = new byte[mac.getMacSize()];
            mac.doFinal(result, 0);
            mac.reset();
            return result;
        }
        catch (IOException e)
        {
            // This should never happen
            throw new IllegalStateException("Internal error during mac calculation");
        }
    }

}
