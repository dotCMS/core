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

import com.dotcms.enterprise.license.bouncycastle.asn1.*;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AttributeCertificate;


public class SignerAttribute
    extends ASN1Encodable
{
    private ASN1Sequence          claimedAttributes;
    private AttributeCertificate  certifiedAttributes;

    public static SignerAttribute getInstance(
        Object o)
    {
        if (o == null || o instanceof SignerAttribute)
        {
            return (SignerAttribute) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new SignerAttribute(o);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SignerAttribute' factory: "
                        + o.getClass().getName() + ".");
    }

    private SignerAttribute(
        Object o)
    {
        ASN1Sequence seq = (ASN1Sequence) o;
        DERTaggedObject taggedObject = (DERTaggedObject) seq.getObjectAt(0);
        if (taggedObject.getTagNo() == 0)
        {
            claimedAttributes = ASN1Sequence.getInstance(taggedObject, true);
        }
        else if (taggedObject.getTagNo() == 1)
        {
            certifiedAttributes = AttributeCertificate.getInstance(taggedObject);
        }
        else
        {
            throw new IllegalArgumentException("illegal tag.");
        }
    }

    public SignerAttribute(
        ASN1Sequence claimedAttributes)
    {
        this.claimedAttributes = claimedAttributes;
    }

    public SignerAttribute(
        AttributeCertificate certifiedAttributes)
    {
        this.certifiedAttributes = certifiedAttributes;
    }

    public ASN1Sequence getClaimedAttributes()
    {
        return claimedAttributes;
    }

    public AttributeCertificate getCertifiedAttributes()
    {
        return certifiedAttributes;
    }

    /**
     *
     * <pre>
     *  SignerAttribute ::= SEQUENCE OF CHOICE {
     *      claimedAttributes   [0] ClaimedAttributes,
     *      certifiedAttributes [1] CertifiedAttributes }
     *
     *  ClaimedAttributes ::= SEQUENCE OF Attribute
     *  CertifiedAttributes ::= AttributeCertificate -- as defined in RFC 3281: see clause 4.1.
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (claimedAttributes != null)
        {
            v.add(new DERTaggedObject(0, claimedAttributes));
        }
        else
        {
            v.add(new DERTaggedObject(1, certifiedAttributes));
        }

        return new DERSequence(v);
    }
}
