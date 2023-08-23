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

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.IOException;

import com.dotcms.enterprise.license.bouncycastle.util.Strings;

/**
 * generic literal data packet.
 */
public class LiteralDataPacket 
    extends InputStreamPacket
{
    int     format;
    byte[]  fileName;
    long    modDate;
    
    LiteralDataPacket(
        BCPGInputStream    in)
        throws IOException
    {
        super(in);
        
        format = in.read();    
        int    l = in.read();
        
        fileName = new byte[l];
        for (int i = 0; i != fileName.length; i++)
        {
            fileName[i] = (byte)in.read();
        }

        modDate = ((long)in.read() << 24) | (in.read() << 16) | (in.read() << 8) | in.read();
    }
    
    /**
     * return the format tag value.
     * 
     * @return format tag value.
     */
    public int getFormat()
    {
        return format;
    }

    /**
     * Return the modification time of the file in milli-seconds.
     * 
     * @return the modification time in millis
     */
    public long getModificationTime()
    {
        return modDate * 1000L;
    }
    
    /**
     * @return filename
     */
    public String getFileName()
    {
        return Strings.fromUTF8ByteArray(fileName);
    }

    public byte[] getRawFileName()
    {
        byte[] tmp = new byte[fileName.length];

        for (int i = 0; i != tmp.length; i++)
        {
            tmp[i] = fileName[i];
        }

        return tmp;
    }
}
