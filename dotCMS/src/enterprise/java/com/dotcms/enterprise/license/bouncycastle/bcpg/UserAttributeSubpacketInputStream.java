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

import java.io.*;

import com.dotcms.enterprise.license.bouncycastle.bcpg.attr.ImageAttribute;

/**
 * reader for user attribute sub-packets
 */
public class UserAttributeSubpacketInputStream
    extends InputStream implements UserAttributeSubpacketTags
{
    InputStream    in;
    
    public UserAttributeSubpacketInputStream(
        InputStream    in)
    {
        this.in = in;
    }
    
    public int available()
        throws IOException
    {
        return in.available();
    }
    
    public int read()
        throws IOException
    {
        return in.read();
    }
    
    private void readFully(
        byte[]    buf,
        int       off,
        int       len)
        throws IOException
    {
        if (len > 0)
        {
            int    b = this.read();
            
            if (b < 0)
            {
                throw new EOFException();
            }
            
            buf[off] = (byte)b;
            off++;
            len--;
        }
        
        while (len > 0)
        {
            int    l = in.read(buf, off, len);
            
            if (l < 0)
            {
                throw new EOFException();
            }
            
            off += l;
            len -= l;
        }
    }
    
    public UserAttributeSubpacket readPacket()
        throws IOException
    {
        int            l = this.read();
        int            bodyLen = 0;
        
        if (l < 0)
        {
            return null;
        }

        if (l < 192)
        {
            bodyLen = l;
        }
        else if (l <= 223)
        {
            bodyLen = ((l - 192) << 8) + (in.read()) + 192;
        }
        else if (l == 255)
        {
            bodyLen = (in.read() << 24) | (in.read() << 16) |  (in.read() << 8)  | in.read();
        }
        else
        {
            // TODO Error?
        }

       int        tag = in.read();

       if (tag < 0)
       {
               throw new EOFException("unexpected EOF reading user attribute sub packet");
       }
       
       byte[]    data = new byte[bodyLen - 1];

       this.readFully(data, 0, data.length);
       
       int       type = tag;

       switch (type)
       {
       case IMAGE_ATTRIBUTE:
           return new ImageAttribute(data);
       }

       return new UserAttributeSubpacket(type, data);
    }
}
