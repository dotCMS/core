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

public class DERBoolean
    extends ASN1Object
{
    byte         value;

    public static final DERBoolean FALSE = new DERBoolean(false);
    public static final DERBoolean TRUE  = new DERBoolean(true);

    /**
     * return a boolean from the passed in object.
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERBoolean getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERBoolean)
        {
            return (DERBoolean)obj;
        }

        if (obj instanceof ASN1OctetString)
        {
            return new DERBoolean(((ASN1OctetString)obj).getOctets());
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject)obj).getObject());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return a DERBoolean from the passed in boolean.
     */
    public static DERBoolean getInstance(
        boolean  value)
    {
        return (value ? TRUE : FALSE);
    }

    /**
     * return a Boolean from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static DERBoolean getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject());
    }
    
    public DERBoolean(
        byte[]       value)
    {
        this.value = value[0];
    }

    public DERBoolean(
        boolean     value)
    {
        this.value = (value) ? (byte)0xff : (byte)0;
    }

    public boolean isTrue()
    {
        return (value != 0);
    }

    void encode(
        DEROutputStream out)
        throws IOException
    {
        byte[]  bytes = new byte[1];

        bytes[0] = value;

        out.writeEncoded(BOOLEAN, bytes);
    }
    
    protected boolean asn1Equals(
        DERObject  o)
    {
        if ((o == null) || !(o instanceof DERBoolean))
        {
            return false;
        }

        return (value == ((DERBoolean)o).value);
    }
    
    public int hashCode()
    {
        return value;
    }


    public String toString()
    {
      return (value != 0) ? "TRUE" : "FALSE";
    }
}
