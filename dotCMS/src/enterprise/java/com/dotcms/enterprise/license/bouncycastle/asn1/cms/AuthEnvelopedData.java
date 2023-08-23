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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Set;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.BERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

public class AuthEnvelopedData
    extends ASN1Encodable
{
    private DERInteger version;
    private OriginatorInfo originatorInfo;
    private ASN1Set recipientInfos;
    private EncryptedContentInfo authEncryptedContentInfo;
    private ASN1Set authAttrs;
    private ASN1OctetString mac;
    private ASN1Set unauthAttrs;

    public AuthEnvelopedData(
        OriginatorInfo originatorInfo,
        ASN1Set recipientInfos,
        EncryptedContentInfo authEncryptedContentInfo,
        ASN1Set authAttrs,
        ASN1OctetString mac,
        ASN1Set unauthAttrs)
    {
        // "It MUST be set to 0."
        this.version = new DERInteger(0);

        this.originatorInfo = originatorInfo;

        // TODO
        // "There MUST be at least one element in the collection."
        this.recipientInfos = recipientInfos;

        this.authEncryptedContentInfo = authEncryptedContentInfo;

        // TODO
        // "The authAttrs MUST be present if the content type carried in
        // EncryptedContentInfo is not id-data."
        this.authAttrs = authAttrs;

        this.mac = mac;

        this.unauthAttrs = unauthAttrs;
    }

    public AuthEnvelopedData(
        ASN1Sequence seq)
    {
        int index = 0;

        // TODO
        // "It MUST be set to 0."
        DERObject tmp = seq.getObjectAt(index++).getDERObject();
        version = (DERInteger)tmp;

        tmp = seq.getObjectAt(index++).getDERObject();
        if (tmp instanceof ASN1TaggedObject)
        {
            originatorInfo = OriginatorInfo.getInstance((ASN1TaggedObject)tmp, false);
            tmp = seq.getObjectAt(index++).getDERObject();
        }

        // TODO
        // "There MUST be at least one element in the collection."
        recipientInfos = ASN1Set.getInstance(tmp);

        tmp = seq.getObjectAt(index++).getDERObject();
        authEncryptedContentInfo = EncryptedContentInfo.getInstance(tmp);

        tmp = seq.getObjectAt(index++).getDERObject();
        if (tmp instanceof ASN1TaggedObject)
        {
            authAttrs = ASN1Set.getInstance((ASN1TaggedObject)tmp, false);
            tmp = seq.getObjectAt(index++).getDERObject();
        }
        else
        {
            // TODO
            // "The authAttrs MUST be present if the content type carried in
            // EncryptedContentInfo is not id-data."
        }

        mac = ASN1OctetString.getInstance(tmp);

        if (seq.size() > index)
        {
            tmp = seq.getObjectAt(index++).getDERObject();
            unauthAttrs = ASN1Set.getInstance((ASN1TaggedObject)tmp, false);
        }
    }

    /**
     * return an AuthEnvelopedData object from a tagged object.
     *
     * @param obj      the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *                 tagged false otherwise.
     * @throws IllegalArgumentException if the object held by the
     *                                  tagged object cannot be converted.
     */
    public static AuthEnvelopedData getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * return an AuthEnvelopedData object from the given object.
     *
     * @param obj the object we want converted.
     * @throws IllegalArgumentException if the object cannot be converted.
     */
    public static AuthEnvelopedData getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof AuthEnvelopedData)
        {
            return (AuthEnvelopedData)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new AuthEnvelopedData((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid AuthEnvelopedData: " + obj.getClass().getName());
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

    public EncryptedContentInfo getAuthEncryptedContentInfo()
    {
        return authEncryptedContentInfo;
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
     * AuthEnvelopedData ::= SEQUENCE {
     *   version CMSVersion,
     *   originatorInfo [0] IMPLICIT OriginatorInfo OPTIONAL,
     *   recipientInfos RecipientInfos,
     *   authEncryptedContentInfo EncryptedContentInfo,
     *   authAttrs [1] IMPLICIT AuthAttributes OPTIONAL,
     *   mac MessageAuthenticationCode,
     *   unauthAttrs [2] IMPLICIT UnauthAttributes OPTIONAL }
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
        v.add(authEncryptedContentInfo);

        // "authAttrs optionally contains the authenticated attributes."
        if (authAttrs != null)
        {
            // "AuthAttributes MUST be DER encoded, even if the rest of the
            // AuthEnvelopedData structure is BER encoded."
            v.add(new DERTaggedObject(false, 1, authAttrs));
        }

        v.add(mac);

        // "unauthAttrs optionally contains the unauthenticated attributes."
        if (unauthAttrs != null)
        {
            v.add(new DERTaggedObject(false, 2, unauthAttrs));
        }

        return new BERSequence(v);
    }
}
