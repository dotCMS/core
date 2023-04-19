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

package com.dotcms.enterprise.license.bouncycastle.asn1;

import com.dotcms.enterprise.license.bouncycastle.util.io.Streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class DERGenerator
    extends ASN1Generator
{       
    private boolean      _tagged = false;
    private boolean      _isExplicit;
    private int          _tagNo;
    
    protected DERGenerator(
        OutputStream out)
    {
        super(out);
    }

    public DERGenerator(
        OutputStream out,
        int          tagNo,
        boolean      isExplicit)
    { 
        super(out);
        
        _tagged = true;
        _isExplicit = isExplicit;
        _tagNo = tagNo;
    }

    private void writeLength(
        OutputStream out,
        int          length)
        throws IOException
    {
        if (length > 127)
        {
            int size = 1;
            int val = length;

            while ((val >>>= 8) != 0)
            {
                size++;
            }

            out.write((byte)(size | 0x80));

            for (int i = (size - 1) * 8; i >= 0; i -= 8)
            {
                out.write((byte)(length >> i));
            }
        }
        else
        {
            out.write((byte)length);
        }
    }

    void writeDEREncoded(
        OutputStream out,
        int          tag,
        byte[]       bytes)
        throws IOException
    {
        out.write(tag);
        writeLength(out, bytes.length);
        out.write(bytes);
    }

    void writeDEREncoded(
        int       tag,
        byte[]    bytes)
        throws IOException
    {
        if (_tagged)
        {
            int tagNum = _tagNo | DERTags.TAGGED;
            
            if (_isExplicit)
            {
                int newTag = _tagNo | DERTags.CONSTRUCTED | DERTags.TAGGED;

                ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                
                writeDEREncoded(bOut, tag, bytes);
                
                writeDEREncoded(_out, newTag, bOut.toByteArray());
            }
            else
            {   
                if ((tag & DERTags.CONSTRUCTED) != 0)
                {
                    writeDEREncoded(_out, tagNum | DERTags.CONSTRUCTED, bytes);
                }
                else
                {
                    writeDEREncoded(_out, tagNum, bytes);
                }
            }
        }
        else
        {
            writeDEREncoded(_out, tag, bytes);
        }
    }
    
    void writeDEREncoded(
        OutputStream out,
        int          tag,
        InputStream  in)
        throws IOException
    {
        writeDEREncoded(out, tag, Streams.readAll(in));
    }
}
