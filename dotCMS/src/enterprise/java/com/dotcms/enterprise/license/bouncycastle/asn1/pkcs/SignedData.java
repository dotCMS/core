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

package com.dotcms.enterprise.license.bouncycastle.asn1.pkcs;

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Set;
import com.dotcms.enterprise.license.bouncycastle.asn1.BERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

/**
 * a PKCS#7 signed data object.
 */
public class SignedData
    extends ASN1Encodable
    implements PKCSObjectIdentifiers
{
    private DERInteger              version;
    private ASN1Set                 digestAlgorithms;
    private ContentInfo             contentInfo;
    private ASN1Set                 certificates;
    private ASN1Set                 crls;
    private ASN1Set                 signerInfos;

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

        throw new IllegalArgumentException("unknown object in factory: " + o);
    }

    public SignedData(
        DERInteger        _version,
        ASN1Set           _digestAlgorithms,
        ContentInfo       _contentInfo,
        ASN1Set           _certificates,
        ASN1Set           _crls,
        ASN1Set           _signerInfos)
    {
        version          = _version;
        digestAlgorithms = _digestAlgorithms;
        contentInfo      = _contentInfo;
        certificates     = _certificates;
        crls             = _crls;
        signerInfos      = _signerInfos;
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
            // an interesting feature of SignedData is that there appear to be varying implementations...
            // for the moment we ignore anything which doesn't fit.
            //
            if (o instanceof DERTaggedObject)
            {
                DERTaggedObject tagged = (DERTaggedObject)o;

                switch (tagged.getTagNo())
                {
                case 0:
                    certificates = ASN1Set.getInstance(tagged, false);
                    break;
                case 1:
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

    public ContentInfo getContentInfo()
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
     *  SignedData ::= SEQUENCE {
     *      version Version,
     *      digestAlgorithms DigestAlgorithmIdentifiers,
     *      contentInfo ContentInfo,
     *      certificates
     *          [0] IMPLICIT ExtendedCertificatesAndCertificates
     *                   OPTIONAL,
     *      crls
     *          [1] IMPLICIT CertificateRevocationLists OPTIONAL,
     *      signerInfos SignerInfos }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(version);
        v.add(digestAlgorithms);
        v.add(contentInfo);

        if (certificates != null)
        {
            v.add(new DERTaggedObject(false, 0, certificates));
        }

        if (crls != null)
        {
            v.add(new DERTaggedObject(false, 1, crls));
        }

        v.add(signerInfos);

        return new BERSequence(v);
    }
}
