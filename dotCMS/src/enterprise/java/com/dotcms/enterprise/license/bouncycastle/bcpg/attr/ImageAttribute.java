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

package com.dotcms.enterprise.license.bouncycastle.bcpg.attr;

import com.dotcms.enterprise.license.bouncycastle.bcpg.UserAttributeSubpacket;
import com.dotcms.enterprise.license.bouncycastle.bcpg.UserAttributeSubpacketTags;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Basic type for a image attribute packet.
 */
public class ImageAttribute 
    extends UserAttributeSubpacket
{
    public static final int JPEG = 1;

    private static final byte[] ZEROES = new byte[12];

    private int     hdrLength;
    private int     version;
    private int     encoding;
    private byte[]  imageData;
    
    public ImageAttribute(
        byte[]    data)
    {
        super(UserAttributeSubpacketTags.IMAGE_ATTRIBUTE, data);
        
        hdrLength = ((data[1] & 0xff) << 8) | (data[0] & 0xff);
        version = data[2] & 0xff;
        encoding = data[3] & 0xff;
        
        imageData = new byte[data.length - hdrLength];
        System.arraycopy(data, hdrLength, imageData, 0, imageData.length);
    }

    public ImageAttribute(
        int imageType,
        byte[] imageData)
    {
        this(toByteArray(imageType, imageData));
    }

    private static byte[] toByteArray(int imageType, byte[] imageData)
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        try
        {
            bOut.write(0x10); bOut.write(0x00); bOut.write(0x01);
            bOut.write(imageType);
            bOut.write(ZEROES);
            bOut.write(imageData);
        }
        catch (IOException e)
        {
            throw new RuntimeException("unable to encode to byte array!");
        }

        return bOut.toByteArray();
    }

    public int version()
    {
        return version;
    }
    
    public int getEncoding()
    {
        return encoding;
    }
    
    public byte[] getImageData()
    {
        return imageData;
    }
}
