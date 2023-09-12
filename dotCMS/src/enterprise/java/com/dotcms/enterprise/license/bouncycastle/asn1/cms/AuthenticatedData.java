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

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Set;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.BERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class AuthenticatedData
    extends ASN1Encodable
{
    private DERInteger version;
    private OriginatorInfo originatorInfo;
    private ASN1Set recipientInfos;
    private AlgorithmIdentifier macAlgorithm;
    private AlgorithmIdentifier digestAlgorithm;
    private ContentInfo encapsulatedContentInfo;
    private ASN1Set authAttrs;
    private ASN1OctetString mac;
    private ASN1Set unauthAttrs;

    public AuthenticatedData(
        OriginatorInfo originatorInfo,
        ASN1Set recipientInfos,
        AlgorithmIdentifier macAlgorithm,
        AlgorithmIdentifier digestAlgorithm,
        ContentInfo encapsulatedContent,
        ASN1Set authAttrs,
        ASN1OctetString mac,
        ASN1Set unauthAttrs)
    {
        if (digestAlgorithm != null || authAttrs != null)
        {
            if (digestAlgorithm == null || authAttrs == null)
            {
                throw new IllegalArgumentException("digestAlgorithm and authAttrs must be set together");
            }
        }

        version = new DERInteger(calculateVersion(originatorInfo));
        
        this.originatorInfo = originatorInfo;
        this.macAlgorithm = macAlgorithm;
        this.digestAlgorithm = digestAlgorithm;
        this.recipientInfos = recipientInfos;
        this.encapsulatedContentInfo = encapsulatedContent;
        this.authAttrs = authAttrs;
        this.mac = mac;
        this.unauthAttrs = unauthAttrs;
    }

    public AuthenticatedData(
        ASN1Sequence seq)
    {
        int index = 0;

        version = (DERInteger)seq.getObjectAt(index++);

        Object tmp = seq.getObjectAt(index++);

        if (tmp instanceof ASN1TaggedObject)
        {
            originatorInfo = OriginatorInfo.getInstance((ASN1TaggedObject)tmp, false);
            tmp = seq.getObjectAt(index++);
        }

        recipientInfos = ASN1Set.getInstance(tmp);
        macAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(index++));

        tmp = seq.getObjectAt(index++);

        if (tmp instanceof ASN1TaggedObject)
        {
            digestAlgorithm = AlgorithmIdentifier.getInstance((ASN1TaggedObject)tmp, false);
            tmp = seq.getObjectAt(index++);
        }

        encapsulatedContentInfo = ContentInfo.getInstance(tmp);

        tmp = seq.getObjectAt(index++);

        if (tmp instanceof ASN1TaggedObject)
        {
            authAttrs = ASN1Set.getInstance((ASN1TaggedObject)tmp, false);
            tmp = seq.getObjectAt(index++);
        }

        mac = ASN1OctetString.getInstance(tmp);
        
        if (seq.size() > index)
        {
            unauthAttrs = ASN1Set.getInstance((ASN1TaggedObject)seq.getObjectAt(index), false);
        }
    }

    /**
     * return an AuthenticatedData object from a tagged object.
     *
     * @param obj      the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *                 tagged false otherwise.
     * @throws IllegalArgumentException if the object held by the
     *                                  tagged object cannot be converted.
     */
    public static AuthenticatedData getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * return an AuthenticatedData object from the given object.
     *
     * @param obj the object we want converted.
     * @throws IllegalArgumentException if the object cannot be converted.
     */
    public static AuthenticatedData getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof AuthenticatedData)
        {
            return (AuthenticatedData)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new AuthenticatedData((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid AuthenticatedData: " + obj.getClass().getName());
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public OriginatorInfo getOriginatorInfo()
    {
        return originatorInfo;
    }

    public ASN1Set getRecipientInfos()
    {
        return recipientInfos;
    }

    public AlgorithmIdentifier getMacAlgorithm()
    {
        return macAlgorithm;
    }

    public ContentInfo getEncapsulatedContentInfo()
    {
        return encapsulatedContentInfo;
    }

    public ASN1Set getAuthAttrs()
    {
        return authAttrs;
    }

    public ASN1OctetString getMac()
    {
        return mac;
    }

    public ASN1Set getUnauthAttrs()
    {
        return unauthAttrs;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * AuthenticatedData ::= SEQUENCE {
     *       version CMSVersion,
     *       originatorInfo [0] IMPLICIT OriginatorInfo OPTIONAL,
     *       recipientInfos RecipientInfos,
     *       macAlgorithm MessageAuthenticationCodeAlgorithm,
     *       digestAlgorithm [1] DigestAlgorithmIdentifier OPTIONAL,
     *       encapContentInfo EncapsulatedContentInfo,
     *       authAttrs [2] IMPLICIT AuthAttributes OPTIONAL,
     *       mac MessageAuthenticationCode,
     *       unauthAttrs [3] IMPLICIT UnauthAttributes OPTIONAL }
     *
     * AuthAttributes ::= SET SIZE (1..MAX) OF Attribute
     *
     * UnauthAttributes ::= SET SIZE (1..MAX) OF Attribute
     *
     * MessageAuthenticationCode ::= OCTET STRING
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(version);

        if (originatorInfo != null)
        {
            v.add(new DERTaggedObject(false, 0, originatorInfo));
        }

        v.add(recipientInfos);
        v.add(macAlgorithm);

        if (digestAlgorithm != null)
        {
            v.add(new DERTaggedObject(false, 1, digestAlgorithm));
        }

        v.add(encapsulatedContentInfo);

        if (authAttrs != null)
        {
            v.add(new DERTaggedObject(false, 2, authAttrs));
        }

        v.add(mac);

        if (unauthAttrs != null)
        {
            v.add(new DERTaggedObject(false, 3, unauthAttrs));
        }

        return new BERSequence(v);
    }

    public static int calculateVersion(OriginatorInfo origInfo)
    {
        if (origInfo == null)
        {
            return 0;
        }
        else
        {
            int ver = 0;

            for (Enumeration e = origInfo.getCertificates().getObjects(); e.hasMoreElements();)
            {
                Object obj = e.nextElement();

                if (obj instanceof ASN1TaggedObject)
                {
                    ASN1TaggedObject tag = (ASN1TaggedObject)obj;

                    if (tag.getTagNo() == 2)
                    {
                        ver = 1;
                    }
                    else if (tag.getTagNo() == 3)
                    {
                        ver = 3;
                        break;
                    }
                }
            }

            for (Enumeration e = origInfo.getCRLs().getObjects(); e.hasMoreElements();)
            {
                Object obj = e.nextElement();

                if (obj instanceof ASN1TaggedObject)
                {
                    ASN1TaggedObject tag = (ASN1TaggedObject)obj;

                    if (tag.getTagNo() == 1)
                    {
                        ver = 3;
                        break;
                    }
                }
            }

            return ver;
        }
    }
}
