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

package com.dotcms.enterprise.license.bouncycastle.asn1.cms;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Set;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.BERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.BERSet;
import com.dotcms.enterprise.license.bouncycastle.asn1.BERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

import java.util.Enumeration;

/**
 * a signed data object.
 */
public class SignedData
    extends ASN1Encodable
{
    private DERInteger  version;
    private ASN1Set     digestAlgorithms;
    private ContentInfo contentInfo;
    private ASN1Set     certificates;
    private ASN1Set     crls;
    private ASN1Set     signerInfos;
    private boolean certsBer;
    private boolean        crlsBer;

    public static SignedData getInstance(
        Object  o)
    {
        if (o instanceof SignedData)
        {
            return (SignedData)o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new SignedData((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }

    public SignedData(
        ASN1Set     digestAlgorithms,
        ContentInfo contentInfo,
        ASN1Set     certificates,
        ASN1Set     crls,
        ASN1Set     signerInfos)
    {
        this.version = calculateVersion(contentInfo.getContentType(), certificates, crls, signerInfos);
        this.digestAlgorithms = digestAlgorithms;
        this.contentInfo = contentInfo;
        this.certificates = certificates;
        this.crls = crls;
        this.signerInfos = signerInfos;
        this.crlsBer = crls instanceof BERSet;
        this.certsBer = certificates instanceof BERSet;
    }


    // RFC3852, section 5.1:
    // IF ((certificates is present) AND
    //    (any certificates with a type of other are present)) OR
    //    ((crls is present) AND
    //    (any crls with a type of other are present))
    // THEN version MUST be 5
    // ELSE
    //    IF (certificates is present) AND
    //       (any version 2 attribute certificates are present)
    //    THEN version MUST be 4
    //    ELSE
    //       IF ((certificates is present) AND
    //          (any version 1 attribute certificates are present)) OR
    //          (any SignerInfo structures are version 3) OR
    //          (encapContentInfo eContentType is other than id-data)
    //       THEN version MUST be 3
    //       ELSE version MUST be 1
    //
    private DERInteger calculateVersion(
        DERObjectIdentifier contentOid,
        ASN1Set certs,
        ASN1Set crls,
        ASN1Set signerInfs)
    {
        boolean otherCert = false;
        boolean otherCrl = false;
        boolean attrCertV1Found = false;
        boolean attrCertV2Found = false;

        if (certs != null)
        {
            for (Enumeration en = certs.getObjects(); en.hasMoreElements();)
            {
                Object obj = en.nextElement();
                if (obj instanceof ASN1TaggedObject)
                {
                    ASN1TaggedObject tagged = (ASN1TaggedObject)obj;

                    if (tagged.getTagNo() == 1)
                    {
                        attrCertV1Found = true;
                    }
                    else if (tagged.getTagNo() == 2)
                    {
                        attrCertV2Found = true;
                    }
                    else if (tagged.getTagNo() == 3)
                    {
                        otherCert = true;
                    }
                }
            }
        }

        if (otherCert)
        {
            return new DERInteger(5);
        }

        if (crls != null)         // no need to check if otherCert is true
        {
            for (Enumeration en = crls.getObjects(); en.hasMoreElements();)
            {
                Object obj = en.nextElement();
                if (obj instanceof ASN1TaggedObject)
                {
                    otherCrl = true;
                }
            }
        }

        if (otherCrl)
        {
            return new DERInteger(5);
        }

        if (attrCertV2Found)
        {
            return new DERInteger(4);
        }

        if (attrCertV1Found)
        {
            return new DERInteger(3);
        }

        if (contentOid.equals(CMSObjectIdentifiers.data))
        {
            if (checkForVersion3(signerInfs))
            {
                return new DERInteger(3);
            }
            else
            {
                return new DERInteger(1);
            }
        }
        else
        {
            return new DERInteger(3);
        }
    }

    private boolean checkForVersion3(ASN1Set signerInfs)
    {
        for (Enumeration e = signerInfs.getObjects(); e.hasMoreElements();)
        {
            SignerInfo s = SignerInfo.getInstance(e.nextElement());

            if (s.getVersion().getValue().intValue() == 3)
            {
                return true;
            }
        }

        return false;
    }

    public SignedData(
        ASN1Sequence seq)
    {
        Enumeration     e = seq.getObjects();

        version = (DERInteger)e.nextElement();
        digestAlgorithms = ((ASN1Set)e.nextElement());
        contentInfo = ContentInfo.getInstance(e.nextElement());

        while (e.hasMoreElements())
        {
            DERObject o = (DERObject)e.nextElement();

            //
            // an interesting feature of SignedData is that there appear
            // to be varying implementations...
            // for the moment we ignore anything which doesn't fit.
            //
            if (o instanceof ASN1TaggedObject)
            {
                ASN1TaggedObject tagged = (ASN1TaggedObject)o;

                switch (tagged.getTagNo())
                {
                case 0:
                    certsBer = tagged instanceof BERTaggedObject;
                    certificates = ASN1Set.getInstance(tagged, false);
                    break;
                case 1:
                    crlsBer = tagged instanceof BERTaggedObject;
                    crls = ASN1Set.getInstance(tagged, false);
                    break;
                default:
                    throw new IllegalArgumentException("unknown tag value " + tagged.getTagNo());
                }
            }
            else
            {
                signerInfos = (ASN1Set)o;
            }
        }
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public ASN1Set getDigestAlgorithms()
    {
        return digestAlgorithms;
    }

    public ContentInfo getEncapContentInfo()
    {
        return contentInfo;
    }

    public ASN1Set getCertificates()
    {
        return certificates;
    }

    public ASN1Set getCRLs()
    {
        return crls;
    }

    public ASN1Set getSignerInfos()
    {
        return signerInfos;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * SignedData ::= SEQUENCE {
     *     version CMSVersion,
     *     digestAlgorithms DigestAlgorithmIdentifiers,
     *     encapContentInfo EncapsulatedContentInfo,
     *     certificates [0] IMPLICIT CertificateSet OPTIONAL,
     *     crls [1] IMPLICIT CertificateRevocationLists OPTIONAL,
     *     signerInfos SignerInfos
     *   }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(digestAlgorithms);
        v.add(contentInfo);

        if (certificates != null)
        {
            if (certsBer)
            {
                v.add(new BERTaggedObject(false, 0, certificates));
            }
            else
            {
                v.add(new DERTaggedObject(false, 0, certificates));
            }
        }

        if (crls != null)
        {
            if (crlsBer)
            {
                v.add(new BERTaggedObject(false, 1, crls));
            }
            else
            {
                v.add(new DERTaggedObject(false, 1, crls));
            }
        }

        v.add(signerInfos);

        return new BERSequence(v);
    }
}
