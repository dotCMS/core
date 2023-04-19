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

package com.dotcms.enterprise.license.bouncycastle.asn1.esf;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUTF8String;

import java.util.Enumeration;

/**
 * Signer-Location attribute (RFC3126).
 * 
 * <pre>
 *   SignerLocation ::= SEQUENCE {
 *       countryName        [0] DirectoryString OPTIONAL,
 *       localityName       [1] DirectoryString OPTIONAL,
 *       postalAddress      [2] PostalAddress OPTIONAL }
 *
 *   PostalAddress ::= SEQUENCE SIZE(1..6) OF DirectoryString
 * </pre>
 */
public class SignerLocation
    extends ASN1Encodable 
{
    private DERUTF8String   countryName;
    private DERUTF8String   localityName;
    private ASN1Sequence    postalAddress;
    
    public SignerLocation(
        ASN1Sequence seq)
    {
        Enumeration     e = seq.getObjects();

        while (e.hasMoreElements())
        {
            DERTaggedObject o = (DERTaggedObject)e.nextElement();

            switch (o.getTagNo())
            {
            case 0:
                this.countryName = DERUTF8String.getInstance(o, true);
                break;
            case 1:
                this.localityName = DERUTF8String.getInstance(o, true);
                break;
            case 2:
                if (o.isExplicit())
                {
                    this.postalAddress = ASN1Sequence.getInstance(o, true);
                }
                else    // handle erroneous implicitly tagged sequences
                {
                    this.postalAddress = ASN1Sequence.getInstance(o, false);
                }
                if (postalAddress != null && postalAddress.size() > 6)
                {
                    throw new IllegalArgumentException("postal address must contain less than 6 strings");
                }
                break;
            default:
                throw new IllegalArgumentException("illegal tag");
            }
        }
    }

    public SignerLocation(
        DERUTF8String   countryName,
        DERUTF8String   localityName,
        ASN1Sequence    postalAddress)
    {
        if (postalAddress != null && postalAddress.size() > 6)
        {
            throw new IllegalArgumentException("postal address must contain less than 6 strings");
        }

        if (countryName != null)
        {
            this.countryName = DERUTF8String.getInstance(countryName.toASN1Object());
        }

        if (localityName != null)
        {
            this.localityName = DERUTF8String.getInstance(localityName.toASN1Object());
        }

        if (postalAddress != null)
        {
            this.postalAddress = ASN1Sequence.getInstance(postalAddress.toASN1Object());
        }
    }

    public static SignerLocation getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SignerLocation)
        {
            return (SignerLocation)obj;
        }

        return new SignerLocation(ASN1Sequence.getInstance(obj));
    }

    public DERUTF8String getCountryName()
    {
        return countryName;
    }

    public DERUTF8String getLocalityName()
    {
        return localityName;
    }

    public ASN1Sequence getPostalAddress()
    {
        return postalAddress;
    }

    /**
     * <pre>
     *   SignerLocation ::= SEQUENCE {
     *       countryName        [0] DirectoryString OPTIONAL,
     *       localityName       [1] DirectoryString OPTIONAL,
     *       postalAddress      [2] PostalAddress OPTIONAL }
     *
     *   PostalAddress ::= SEQUENCE SIZE(1..6) OF DirectoryString
     *   
     *   DirectoryString ::= CHOICE {
     *         teletexString           TeletexString (SIZE (1..MAX)),
     *         printableString         PrintableString (SIZE (1..MAX)),
     *         universalString         UniversalString (SIZE (1..MAX)),
     *         utf8String              UTF8String (SIZE (1.. MAX)),
     *         bmpString               BMPString (SIZE (1..MAX)) }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        if (countryName != null)
        {
            v.add(new DERTaggedObject(true, 0, countryName));
        }

        if (localityName != null)
        {
            v.add(new DERTaggedObject(true, 1, localityName));
        }

        if (postalAddress != null)
        {
            v.add(new DERTaggedObject(true, 2, postalAddress));
        }

        return new DERSequence(v);
    }
}
