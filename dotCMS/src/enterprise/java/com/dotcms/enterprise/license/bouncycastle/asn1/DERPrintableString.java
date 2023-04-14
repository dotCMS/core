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

/**
 * DER PrintableString object.
 */
public class DERPrintableString
    extends ASN1Object
    implements DERString
{
    String  string;

    /**
     * return a printable string from the passed in object.
     * 
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERPrintableString getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERPrintableString)
        {
            return (DERPrintableString)obj;
        }

        if (obj instanceof ASN1OctetString)
        {
            return new DERPrintableString(((ASN1OctetString)obj).getOctets());
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject)obj).getObject());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return a Printable String from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static DERPrintableString getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject());
    }

    /**
     * basic constructor - byte encoded string.
     */
    public DERPrintableString(
        byte[]   string)
    {
        char[]  cs = new char[string.length];

        for (int i = 0; i != cs.length; i++)
        {
            cs[i] = (char)(string[i] & 0xff);
        }

        this.string = new String(cs);
    }

    /**
     * basic constructor - this does not validate the string
     */
    public DERPrintableString(
        String   string)
    {
        this(string, false);
    }

    /**
     * Constructor with optional validation.
     *
     * @param string the base string to wrap.
     * @param validate whether or not to check the string.
     * @throws IllegalArgumentException if validate is true and the string
     * contains characters that should not be in a PrintableString.
     */
    public DERPrintableString(
        String   string,
        boolean  validate)
    {
        if (validate && !isPrintableString(string))
        {
            throw new IllegalArgumentException("string contains illegal characters");
        }

        this.string = string;
    }

    public String getString()
    {
        return string;
    }

    public byte[] getOctets()
    {
        char[]  cs = string.toCharArray();
        byte[]  bs = new byte[cs.length];

        for (int i = 0; i != cs.length; i++)
        {
            bs[i] = (byte)cs[i];
        }

        return bs; 
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        out.writeEncoded(PRINTABLE_STRING, this.getOctets());
    }

    public int hashCode()
    {
        return this.getString().hashCode();
    }

    boolean asn1Equals(
        DERObject  o)
    {
        if (!(o instanceof DERPrintableString))
        {
            return false;
        }

        DERPrintableString  s = (DERPrintableString)o;

        return this.getString().equals(s.getString());
    }

    public String toString()
    {
        return string;
    }

    /**
     * return true if the passed in String can be represented without
     * loss as a PrintableString, false otherwise.
     *
     * @return true if in printable set, false otherwise.
     */
    public static boolean isPrintableString(
        String  str)
    {
        for (int i = str.length() - 1; i >= 0; i--)
        {
            char    ch = str.charAt(i);

            if (ch > 0x007f)
            {
                return false;
            }

            if ('a' <= ch && ch <= 'z')
            {
                continue;
            }

            if ('A' <= ch && ch <= 'Z')
            {
                continue;
            }

            if ('0' <= ch && ch <= '9')
            {
                continue;
            }

            switch (ch)
            {
            case ' ':
            case '\'':
            case '(':
            case ')':
            case '+':
            case '-':
            case '.':
            case ':':
            case '=':
            case '?':
            case '/':
            case ',':
                continue;
            }

            return false;
        }

        return true;
    }
}
