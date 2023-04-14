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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1SequenceParser;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1SetParser;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObjectParser;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTags;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.io.IOException;

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
public class AuthenticatedDataParser
{
    private ASN1SequenceParser seq;
    private DERInteger version;
    private DEREncodable nextObject;
    private boolean originatorInfoCalled;

    public AuthenticatedDataParser(
        ASN1SequenceParser seq)
        throws IOException
    {
        this.seq = seq;
        this.version = (DERInteger)seq.readObject();
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public OriginatorInfo getOriginatorInfo()
        throws IOException
    {
        originatorInfoCalled = true;

        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject instanceof ASN1TaggedObjectParser && ((ASN1TaggedObjectParser)nextObject).getTagNo() == 0)
        {
            ASN1SequenceParser originatorInfo = (ASN1SequenceParser) ((ASN1TaggedObjectParser)nextObject).getObjectParser(DERTags.SEQUENCE, false);
            nextObject = null;
            return OriginatorInfo.getInstance(originatorInfo.getDERObject());
        }

        return null;
    }

    public ASN1SetParser getRecipientInfos()
        throws IOException
    {
        if (!originatorInfoCalled)
        {
            getOriginatorInfo();
        }

        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        ASN1SetParser recipientInfos = (ASN1SetParser)nextObject;
        nextObject = null;
        return recipientInfos;
    }

    public AlgorithmIdentifier getMacAlgorithm()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            ASN1SequenceParser o = (ASN1SequenceParser)nextObject;
            nextObject = null;
            return AlgorithmIdentifier.getInstance(o.getDERObject());
        }

        return null;
    }

    public ContentInfoParser getEnapsulatedContentInfo()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            ASN1SequenceParser o = (ASN1SequenceParser)nextObject;
            nextObject = null;
            return new ContentInfoParser(o);
        }

        return null;
    }

    public ASN1SetParser getAuthAttrs()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject instanceof ASN1TaggedObjectParser)
        {
            DEREncodable o = nextObject;
            nextObject = null;
            return (ASN1SetParser)((ASN1TaggedObjectParser)o).getObjectParser(DERTags.SET, false);
        }

        return null;
    }

    public ASN1OctetString getMac()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        DEREncodable o = nextObject;
        nextObject = null;

        return ASN1OctetString.getInstance(o.getDERObject());
    }

    public ASN1SetParser getUnauthAttrs()
        throws IOException
    {
        if (nextObject == null)
        {
            nextObject = seq.readObject();
        }

        if (nextObject != null)
        {
            DEREncodable o = nextObject;
            nextObject = null;
            return (ASN1SetParser)((ASN1TaggedObjectParser)o).getObjectParser(DERTags.SET, false);
        }

        return null;
    }
}
