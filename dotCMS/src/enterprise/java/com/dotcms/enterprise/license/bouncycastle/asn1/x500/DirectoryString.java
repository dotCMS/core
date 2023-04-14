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

package com.dotcms.enterprise.license.bouncycastle.asn1.x500;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBMPString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERPrintableString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERT61String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUTF8String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUniversalString;

public class DirectoryString
    extends ASN1Encodable
    implements ASN1Choice, DERString
{
    private DERString string;

    public static DirectoryString getInstance(Object o)
    {
        if (o instanceof DirectoryString)
        {
            return (DirectoryString)o;
        }

        if (o instanceof DERT61String)
        {
            return new DirectoryString((DERT61String)o);
        }

        if (o instanceof DERPrintableString)
        {
            return new DirectoryString((DERPrintableString)o);
        }

        if (o instanceof DERUniversalString)
        {
            return new DirectoryString((DERUniversalString)o);
        }

        if (o instanceof DERUTF8String)
        {
            return new DirectoryString((DERUTF8String)o);
        }

        if (o instanceof DERBMPString)
        {
            return new DirectoryString((DERBMPString)o);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + o.getClass().getName());
    }

    public static DirectoryString getInstance(ASN1TaggedObject o, boolean explicit)
    {
        if (!explicit)
        {
            throw new IllegalArgumentException("choice item must be explicitly tagged");
        }

        return getInstance(o.getObject());
    }

    private DirectoryString(
        DERT61String string)
    {
        this.string = string;
    }

    private DirectoryString(
        DERPrintableString string)
    {
        this.string = string;
    }

    private DirectoryString(
        DERUniversalString string)
    {
        this.string = string;
    }

    private DirectoryString(
        DERUTF8String string)
    {
        this.string = string;
    }

    private DirectoryString(
        DERBMPString string)
    {
        this.string = string;
    }

    public DirectoryString(String string)
    {
        this.string = new DERUTF8String(string);
    }

    public String getString()
    {
        return string.getString();
    }

    public String toString()
    {
        return string.getString();
    }

    /**
     * <pre>
     *  DirectoryString ::= CHOICE {
     *    teletexString               TeletexString (SIZE (1..MAX)),
     *    printableString             PrintableString (SIZE (1..MAX)),
     *    universalString             UniversalString (SIZE (1..MAX)),
     *    utf8String                  UTF8String (SIZE (1..MAX)),
     *    bmpString                   BMPString (SIZE (1..MAX))  }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return ((DEREncodable)string).getDERObject();
    }
}
