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
import com.dotcms.enterprise.license.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

public abstract class ASN1OctetString
    extends ASN1Object
    implements ASN1OctetStringParser
{
    byte[]  string;

    /**
     * return an Octet String from a tagged object.
     *
     * @param obj the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *              be converted.
     */
    public static ASN1OctetString getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(obj.getObject());
    }
    
    /**
     * return an Octet String from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static ASN1OctetString getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ASN1OctetString)
        {
            return (ASN1OctetString)obj;
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject)obj).getObject());
        }

        if (obj instanceof ASN1Sequence)
        {
            Vector      v = new Vector();
            Enumeration e = ((ASN1Sequence)obj).getObjects();

            while (e.hasMoreElements())
            {
                v.addElement(e.nextElement());
            }

            return new BERConstructedOctetString(v);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * @param string the octets making up the octet string.
     */
    public ASN1OctetString(
        byte[]  string)
    {
        if (string == null)
        {
            throw new NullPointerException("string cannot be null");
        }
        this.string = string;
    }

    public ASN1OctetString(
        DEREncodable obj)
    {
        try
        {
            this.string = obj.getDERObject().getEncoded(ASN1Encodable.DER);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Error processing object : " + e.toString());
        }
    }

    public InputStream getOctetStream()
    {
        return new ByteArrayInputStream(string);
    }

    public ASN1OctetStringParser parser()
    {
        return this;
    }

    public byte[] getOctets()
    {
        return string;
    }

    public int hashCode()
    {
        return Arrays.hashCode(this.getOctets());
    }

    boolean asn1Equals(
        DERObject  o)
    {
        if (!(o instanceof ASN1OctetString))
        {
            return false;
        }

        ASN1OctetString  other = (ASN1OctetString)o;

        return Arrays.areEqual(string, other.string);
    }

    abstract void encode(DEROutputStream out)
        throws IOException;

    public String toString()
    {
      return "#"+new String(Hex.encode(string));
    }
}
