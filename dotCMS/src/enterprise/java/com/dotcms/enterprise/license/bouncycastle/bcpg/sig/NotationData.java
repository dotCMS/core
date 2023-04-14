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
import com.dotcms.enterprise.license.bouncycastle.util.Strings;

import java.io.ByteArrayOutputStream;

/**
 * Class provided a NotationData object according to
 * RFC2440, Chapter 5.2.3.15. Notation Data
 */
public class NotationData
    extends SignatureSubpacket
{
    public static final int HEADER_FLAG_LENGTH = 4;
    public static final int HEADER_NAME_LENGTH = 2;
    public static final int HEADER_VALUE_LENGTH = 2;

    public NotationData(boolean critical, byte[] data)
    {
        super(SignatureSubpacketTags.NOTATION_DATA, critical, data);
    }

    public NotationData(
        boolean critical,
        boolean humanReadable,
        String notationName,
        String notationValue)
    {
        super(SignatureSubpacketTags.NOTATION_DATA, critical, createData(humanReadable, notationName, notationValue));
    }

    private static byte[] createData(boolean humanReadable, String notationName, String notationValue)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

//        (4 octets of flags, 2 octets of name length (M),
//        2 octets of value length (N),
//        M octets of name data,
//        N octets of value data)

        // flags
        out.write(humanReadable ? 0x80 : 0x00);
        out.write(0x0);
        out.write(0x0);
        out.write(0x0);

        byte[] nameData, valueData = null;
        int nameLength, valueLength;

        nameData = Strings.toUTF8ByteArray(notationName);
        nameLength = Math.min(nameData.length, 0xFF);

        valueData = Strings.toUTF8ByteArray(notationValue);
        valueLength = Math.min(valueData.length, 0xFF);

        // name length
        out.write((nameLength >>> 8) & 0xFF);
        out.write((nameLength >>> 0) & 0xFF);

        // value length
        out.write((valueLength >>> 8) & 0xFF);
        out.write((valueLength >>> 0) & 0xFF);

        // name
        out.write(nameData, 0, nameLength);

        // value
        out.write(valueData, 0, valueLength);

        return out.toByteArray();
    }

    public boolean isHumanReadable()
    {
        return data[0] == (byte)0x80;
    }

    public String getNotationName()
    {
        int nameLength = ((data[HEADER_FLAG_LENGTH] << 8) + (data[HEADER_FLAG_LENGTH + 1] << 0));

        byte bName[] = new byte[nameLength];
        System.arraycopy(data, HEADER_FLAG_LENGTH + HEADER_NAME_LENGTH + HEADER_VALUE_LENGTH, bName, 0, nameLength);

        return Strings.fromUTF8ByteArray(bName);
    }

    public String getNotationValue()
    {
        return Strings.fromUTF8ByteArray(getNotationValueBytes());
    }

    public byte[] getNotationValueBytes()
    {
        int nameLength = ((data[HEADER_FLAG_LENGTH] << 8) + (data[HEADER_FLAG_LENGTH + 1] << 0));
        int valueLength = ((data[HEADER_FLAG_LENGTH + HEADER_NAME_LENGTH] << 8) + (data[HEADER_FLAG_LENGTH + HEADER_NAME_LENGTH + 1] << 0));

        byte bValue[] = new byte[valueLength];
        System.arraycopy(data, HEADER_FLAG_LENGTH + HEADER_NAME_LENGTH + HEADER_VALUE_LENGTH + nameLength, bValue, 0, valueLength);
        return bValue;
    }
}
