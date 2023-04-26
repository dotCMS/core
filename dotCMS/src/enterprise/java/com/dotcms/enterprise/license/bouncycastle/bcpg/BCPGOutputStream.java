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

/**
 * Basic output stream.
 */
public class BCPGOutputStream
    extends OutputStream
    implements PacketTags, CompressionAlgorithmTags
{
    OutputStream    out;
    private byte[]  partialBuffer;
    private int     partialBufferLength;
    private int     partialPower;
    private int     partialOffset;
    
    private static final int    BUF_SIZE_POWER = 16; // 2^16 size buffer on long files
    
    public BCPGOutputStream(
        OutputStream    out)
    {
        this.out = out;
    }
    
    /**
     * Create a stream representing an old style partial object.
     * 
     * @param tag the packet tag for the object.
     */
    public BCPGOutputStream(
        OutputStream    out,
        int             tag)
        throws IOException
    {
        this.out = out;
        this.writeHeader(tag, true, true, 0);
    }
    
    /**
     * Create a stream representing a general packet.
     * 
     * @param out
     * @param tag
     * @param length
     * @param oldFormat
     * @throws IOException
     */
    public BCPGOutputStream(
        OutputStream    out,
        int             tag,
        long            length,
        boolean         oldFormat)
        throws IOException
    {
        this.out = out;
        
        if (length > 0xFFFFFFFFL)
        {
            this.writeHeader(tag, false, true, 0);
            this.partialBufferLength = 1 << BUF_SIZE_POWER;
            this.partialBuffer = new byte[partialBufferLength];
            this.partialPower = BUF_SIZE_POWER;
            this.partialOffset = 0;
        }
        else
        {
            this.writeHeader(tag, oldFormat, false, length);
        }
    }
    
    /**
     * 
     * @param tag
     * @param length
     * @throws IOException
     */
    public BCPGOutputStream(
        OutputStream    out,
        int             tag,
        long            length)
        throws IOException
    {
        this.out = out;
        
        this.writeHeader(tag, false, false, length);
    }
    
    /**
     * Create a new style partial input stream buffered into chunks.
     * 
     * @param out output stream to write to.
     * @param tag packet tag.
     * @param buffer size of chunks making up the packet.
     * @throws IOException
     */
    public BCPGOutputStream(
        OutputStream    out,
        int             tag,
        byte[]          buffer)
        throws IOException
    {
        this.out = out;
        this.writeHeader(tag, false, true, 0);
        
        this.partialBuffer = buffer;
        
        int    length = partialBuffer.length;
        
        for (partialPower = 0; length != 1; partialPower++)
        {
            length >>>= 1;
        }
        
        if (partialPower > 30)
        {
            throw new IOException("Buffer cannot be greater than 2^30 in length.");
        }
        
        this.partialBufferLength = 1 << partialPower;
        this.partialOffset = 0;
    }
    
    private void writeNewPacketLength(
        long            bodyLen) 
        throws IOException
    {
        if (bodyLen < 192)
        {
            out.write((byte)bodyLen);
        }
        else if (bodyLen <= 8383)
        {
            bodyLen -= 192;
                    
            out.write((byte)(((bodyLen >> 8) & 0xff) + 192));
            out.write((byte)bodyLen);
        }
        else
        {
            out.write(0xff);
            out.write((byte)(bodyLen >> 24));
            out.write((byte)(bodyLen >> 16));
            out.write((byte)(bodyLen >> 8));
            out.write((byte)bodyLen);
        }
    }
    
    private void writeHeader(
        int        tag,
        boolean    oldPackets,
        boolean    partial,
        long       bodyLen) 
        throws IOException
    {
        int    hdr = 0x80;
        
        if (partialBuffer != null)
        {
            partialFlush(true);
            partialBuffer = null;
        }
        
        if (oldPackets)
        {
            hdr |= tag << 2;
            
            if (partial)
            {
                this.write(hdr | 0x03);
            }
            else
            {
                if (bodyLen <= 0xff)
                {
                    this.write(hdr);
                    this.write((byte)bodyLen);
                }
                else if (bodyLen <= 0xffff)
                {
                    this.write(hdr | 0x01);
                    this.write((byte)(bodyLen >> 8));
                    this.write((byte)(bodyLen));
                }
                else
                {
                    this.write(hdr | 0x02);
                    this.write((byte)(bodyLen >> 24));
                    this.write((byte)(bodyLen >> 16));
                    this.write((byte)(bodyLen >> 8));
                    this.write((byte)bodyLen);
                }
            }
        }
        else
        {
            hdr |= 0x40 | tag;
            this.write(hdr);
            
            if (partial)
            {
                partialOffset = 0;
            }
            else
            {
                this.writeNewPacketLength(bodyLen);
            }
        }
    }
    
    private void partialFlush(
        boolean isLast) 
        throws IOException
    {
        if (isLast)
        {
            writeNewPacketLength(partialOffset);
            out.write(partialBuffer, 0, partialOffset);
        }
        else
        {
            out.write(0xE0 | partialPower);
            out.write(partialBuffer, 0, partialBufferLength);
        }
        
        partialOffset = 0;
    }
    
    private void writePartial(
        byte    b) 
        throws IOException
    {
        if (partialOffset == partialBufferLength)
        {
            partialFlush(false);
        }
        
        partialBuffer[partialOffset++] = b;
    }
    
    private void writePartial(
        byte[]  buf,
        int     off,
        int     len) 
        throws IOException
    {
        if (partialOffset == partialBufferLength)
        {
            partialFlush(false);
        }
        
        if (len <= (partialBufferLength - partialOffset))
        {
            System.arraycopy(buf, off, partialBuffer, partialOffset, len);
            partialOffset += len;
        }
        else
        {
            System.arraycopy(buf, off, partialBuffer, partialOffset, partialBufferLength - partialOffset);
            off += partialBufferLength - partialOffset;
            len -= partialBufferLength - partialOffset;
            partialFlush(false);
            
            while (len > partialBufferLength)
            {
                System.arraycopy(buf, off, partialBuffer, 0, partialBufferLength);
                off += partialBufferLength;
                len -= partialBufferLength;
                partialFlush(false);
            }

            System.arraycopy(buf, off, partialBuffer, 0, len);
            partialOffset += len;
        }
    }
    
    public void write(
        int    b)
        throws IOException
    {
        if (partialBuffer != null)
        {
            writePartial((byte)b);
        }
        else
        {
            out.write(b);
        }
    }
    
    public void write(
        byte[]    bytes,
        int       off,
        int       len)
        throws IOException
    {
        if (partialBuffer != null)
        {
            writePartial(bytes, off, len);
        }
        else
        {
            out.write(bytes, off, len);
        }
    }
    
    public void writePacket(
        ContainedPacket    p)
        throws IOException
    {
        p.encode(this);
    }
    
    void writePacket(
        int        tag,
        byte[]     body,
        boolean    oldFormat)
        throws IOException
    {
        this.writeHeader(tag, oldFormat, false, body.length);
        this.write(body);
    }
    
    public void writeObject(
        BCPGObject    o)
        throws IOException
    {
        o.encode(this);
    }
    
    /**
     * Flush the underlying stream.
     */
    public void flush()
        throws IOException
    {
        out.flush();
    }
    
    /**
     * Finish writing out the current packet without closing the underlying stream.
     */
    public void finish() 
        throws IOException
    {
        if (partialBuffer != null)
        {
            partialFlush(true);
            partialBuffer = null;
        }
    }
    
    public void close()
        throws IOException
    {
        this.finish();
        out.flush();
        out.close();
    }
}
