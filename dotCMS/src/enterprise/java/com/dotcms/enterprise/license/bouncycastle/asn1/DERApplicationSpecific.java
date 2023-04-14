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

import com.dotcms.enterprise.license.bouncycastle.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Base class for an application specific object
 */
public class DERApplicationSpecific 
    extends ASN1Object
{
    private final boolean   isConstructed;
    private final int       tag;
    private final byte[]    octets;

    DERApplicationSpecific(
        boolean isConstructed,
        int     tag,
        byte[]  octets)
    {
        this.isConstructed = isConstructed;
        this.tag = tag;
        this.octets = octets;
    }

    public DERApplicationSpecific(
        int    tag,
        byte[] octets)
    {
        this(false, tag, octets);
    }

    public DERApplicationSpecific(
        int                  tag, 
        DEREncodable         object) 
        throws IOException 
    {
        this(true, tag, object);
    }

    public DERApplicationSpecific(
        boolean      explicit,
        int          tag,
        DEREncodable object)
        throws IOException
    {
        byte[] data = object.getDERObject().getDEREncoded();

        this.isConstructed = explicit;
        this.tag = tag;

        if (explicit)
        {
            this.octets = data;
        }
        else
        {
            int lenBytes = getLengthOfLength(data);
            byte[] tmp = new byte[data.length - lenBytes];
            System.arraycopy(data, lenBytes, tmp, 0, tmp.length);
            this.octets = tmp;
        }
    }

    public DERApplicationSpecific(int tagNo, ASN1EncodableVector vec)
    {
        this.tag = tagNo;
        this.isConstructed = true;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        for (int i = 0; i != vec.size(); i++)
        {
            try
            {
                bOut.write(((ASN1Encodable)vec.get(i)).getEncoded());
            }
            catch (IOException e)
            {
                throw new ASN1ParsingException("malformed object: " + e, e);
            }
        }
        this.octets = bOut.toByteArray();
    }

    private int getLengthOfLength(byte[] data)
    {
        int count = 2;               // TODO: assumes only a 1 byte tag number

        while((data[count - 1] & 0x80) != 0)
        {
            count++;
        }

        return count;
    }

    public boolean isConstructed()
    {
        return isConstructed;
    }
    
    public byte[] getContents()
    {
        return octets;
    }
    
    public int getApplicationTag() 
    {
        return tag;
    }

    /**
     * Return the enclosed object assuming explicit tagging.
     *
     * @return  the resulting object
     * @throws IOException if reconstruction fails.
     */
    public DERObject getObject() 
        throws IOException 
    {
        return new ASN1InputStream(getContents()).readObject();
    }

    /**
     * Return the enclosed object assuming implicit tagging.
     *
     * @param derTagNo the type tag that should be applied to the object's contents.
     * @return  the resulting object
     * @throws IOException if reconstruction fails.
     */
    public DERObject getObject(int derTagNo)
        throws IOException
    {
        if (derTagNo >= 0x1f)
        {
            throw new IOException("unsupported tag number");
        }

        byte[] orig = this.getEncoded();
        byte[] tmp = replaceTagNumber(derTagNo, orig);

        if ((orig[0] & DERTags.CONSTRUCTED) != 0)
        {
            tmp[0] |= DERTags.CONSTRUCTED;
        }

        return new ASN1InputStream(tmp).readObject();
    }
    
    /* (non-Javadoc)
     * @see org.bouncycastle.asn1.DERObject#encode(org.bouncycastle.asn1.DEROutputStream)
     */
    void encode(DEROutputStream out) throws IOException
    {
        int classBits = DERTags.APPLICATION;
        if (isConstructed)
        {
            classBits |= DERTags.CONSTRUCTED; 
        }

        out.writeEncoded(classBits, tag, octets);
    }
    
    boolean asn1Equals(
        DERObject o)
    {
        if (!(o instanceof DERApplicationSpecific))
        {
            return false;
        }

        DERApplicationSpecific other = (DERApplicationSpecific)o;

        return isConstructed == other.isConstructed
            && tag == other.tag
            && Arrays.areEqual(octets, other.octets);
    }

    public int hashCode()
    {
        return (isConstructed ? 1 : 0) ^ tag ^ Arrays.hashCode(octets);
    }

    private byte[] replaceTagNumber(int newTag, byte[] input)
        throws IOException
    {
        int tagNo = input[0] & 0x1f;
        int index = 1;
        //
        // with tagged object tag number is bottom 5 bits, or stored at the start of the content
        //
        if (tagNo == 0x1f)
        {
            tagNo = 0;

            int b = input[index++] & 0xff;

            // X.690-0207 8.1.2.4.2
            // "c) bits 7 to 1 of the first subsequent octet shall not all be zero."
            if ((b & 0x7f) == 0) // Note: -1 will pass
            {
                throw new ASN1ParsingException("corrupted stream - invalid high tag number found");
            }

            while ((b >= 0) && ((b & 0x80) != 0))
            {
                tagNo |= (b & 0x7f);
                tagNo <<= 7;
                b = input[index++] & 0xff;
            }

            tagNo |= (b & 0x7f);
        }

        byte[] tmp = new byte[input.length - index + 1];

        System.arraycopy(input, index, tmp, 1, tmp.length - 1);

        tmp[0] = (byte)newTag;

        return tmp;
    }
}
