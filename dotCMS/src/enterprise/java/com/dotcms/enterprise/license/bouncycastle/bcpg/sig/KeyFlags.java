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

package com.dotcms.enterprise.license.bouncycastle.bcpg.sig;

import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacket;
import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacketTags;

/**
 * Packet holding the key flag values.
 */
public class KeyFlags 
    extends SignatureSubpacket
{
    public static final int CERTIFY_OTHER = 0x01;
    public static final int SIGN_DATA = 0x02;
    public static final int ENCRYPT_COMMS = 0x04;
    public static final int ENCRYPT_STORAGE = 0x08;
    public static final int SPLIT = 0x10;
    public static final int AUTHENTICATION = 0x20;
    public static final int SHARED = 0x80;
    
    private static byte[] intToByteArray(
        int    v)
    {
        byte[] tmp = new byte[4];
        int    size = 0;

        for (int i = 0; i != 4; i++)
        {
            tmp[i] = (byte)(v >> (i * 8));
            if (tmp[i] != 0)
            {
                size = i;
            }
        }

        byte[]    data = new byte[size + 1];
        
        System.arraycopy(tmp, 0, data, 0, data.length);

        return data;
    }
    
    public KeyFlags(
        boolean    critical,
        byte[]     data)
    {
        super(SignatureSubpacketTags.KEY_FLAGS, critical, data);
    }
    
    public KeyFlags(
        boolean    critical,
        int        flags)
    {
        super(SignatureSubpacketTags.KEY_FLAGS, critical, intToByteArray(flags));
    }

    /**
     * Return the flag values contained in the first 4 octets (note: at the moment
     * the standard only uses the first one).
     *
     * @return flag values.
     */
    public int getFlags()
    {
        int flags = 0;

        for (int i = 0; i != data.length; i++)
        {
            flags |= (data[i] & 0xff) << (i * 8);
        }

        return flags;
    }
}
