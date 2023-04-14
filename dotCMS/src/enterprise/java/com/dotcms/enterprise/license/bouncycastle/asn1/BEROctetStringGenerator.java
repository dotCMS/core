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

import java.io.IOException;
import java.io.OutputStream;

public class BEROctetStringGenerator
    extends BERGenerator
{
    public BEROctetStringGenerator(OutputStream out) 
        throws IOException
    {
        super(out);
        
        writeBERHeader(DERTags.CONSTRUCTED | DERTags.OCTET_STRING);
    }

    public BEROctetStringGenerator(
        OutputStream out,
        int tagNo,
        boolean isExplicit) 
        throws IOException
    {
        super(out, tagNo, isExplicit);
        
        writeBERHeader(DERTags.CONSTRUCTED | DERTags.OCTET_STRING);
    }
    
    public OutputStream getOctetOutputStream()
    {
        return getOctetOutputStream(new byte[1000]); // limit for CER encoding.
    }

    public OutputStream getOctetOutputStream(
        byte[] buf)
    {
        return new BufferedBEROctetStream(buf);
    }
   
    private class BufferedBEROctetStream
        extends OutputStream
    {
        private byte[] _buf;
        private int    _off;
        private DEROutputStream _derOut;

        BufferedBEROctetStream(
            byte[] buf)
        {
            _buf = buf;
            _off = 0;
            _derOut = new DEROutputStream(_out);
        }
        
        public void write(
            int b)
            throws IOException
        {
            _buf[_off++] = (byte)b;

            if (_off == _buf.length)
            {
                DEROctetString.encode(_derOut, _buf);
                _off = 0;
            }
        }

        public void write(byte[] b, int off, int len) throws IOException
        {
            while (len > 0)
            {
                int numToCopy = Math.min(len, _buf.length - _off);
                System.arraycopy(b, off, _buf, _off, numToCopy);

                _off += numToCopy;
                if (_off < _buf.length)
                {
                    break;
                }

                DEROctetString.encode(_derOut, _buf);
                _off = 0;

                off += numToCopy;
                len -= numToCopy;
            }
        }

        public void close() 
            throws IOException
        {
            if (_off != 0)
            {
                byte[] bytes = new byte[_off];
                System.arraycopy(_buf, 0, bytes, 0, _off);
                
                DEROctetString.encode(_derOut, bytes);
            }
            
             writeBEREnd();
        }
    }
}
