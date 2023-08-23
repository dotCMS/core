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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.util.Enumeration;

public class SignerInfo
    extends ASN1Encodable
{
    private DERInteger              version;
    private SignerIdentifier        sid;
    private AlgorithmIdentifier     digAlgorithm;
    private ASN1Set                 authenticatedAttributes;
    private AlgorithmIdentifier     digEncryptionAlgorithm;
    private ASN1OctetString         encryptedDigest;
    private ASN1Set                 unauthenticatedAttributes;

    public static SignerInfo getInstance(
        Object  o)
        throws IllegalArgumentException
    {
        if (o == null || o instanceof SignerInfo)
        {
            return (SignerInfo)o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new SignerInfo((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }

    public SignerInfo(
        SignerIdentifier        sid,
        AlgorithmIdentifier     digAlgorithm,
        ASN1Set                 authenticatedAttributes,
        AlgorithmIdentifier     digEncryptionAlgorithm,
        ASN1OctetString         encryptedDigest,
        ASN1Set                 unauthenticatedAttributes)
    {
        if (sid.isTagged())
        {
            this.version = new DERInteger(3);
        }
        else
        {
            this.version = new DERInteger(1);
        }

        this.sid = sid;
        this.digAlgorithm = digAlgorithm;
        this.authenticatedAttributes = authenticatedAttributes;
        this.digEncryptionAlgorithm = digEncryptionAlgorithm;
        this.encryptedDigest = encryptedDigest;
        this.unauthenticatedAttributes = unauthenticatedAttributes;
    }

    public SignerInfo(
        ASN1Sequence seq)
    {
        Enumeration     e = seq.getObjects();

        version = (DERInteger)e.nextElement();
        sid = SignerIdentifier.getInstance(e.nextElement());
        digAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());

        Object obj = e.nextElement();

        if (obj instanceof ASN1TaggedObject)
        {
            authenticatedAttributes = ASN1Set.getInstance((ASN1TaggedObject)obj, false);

            digEncryptionAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        }
        else
        {
            authenticatedAttributes = null;
            digEncryptionAlgorithm = AlgorithmIdentifier.getInstance(obj);
        }

        encryptedDigest = DEROctetString.getInstance(e.nextElement());

        if (e.hasMoreElements())
        {
            unauthenticatedAttributes = ASN1Set.getInstance((ASN1TaggedObject)e.nextElement(), false);
        }
        else
        {
            unauthenticatedAttributes = null;
        }
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public SignerIdentifier getSID()
    {
        return sid;
    }

    public ASN1Set getAuthenticatedAttributes()
    {
        return authenticatedAttributes;
    }

    public AlgorithmIdentifier getDigestAlgorithm()
    {
        return digAlgorithm;
    }

    public ASN1OctetString getEncryptedDigest()
    {
        return encryptedDigest;
    }

    public AlgorithmIdentifier getDigestEncryptionAlgorithm()
    {
        return digEncryptionAlgorithm;
    }

    public ASN1Set getUnauthenticatedAttributes()
    {
        return unauthenticatedAttributes;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  SignerInfo ::= SEQUENCE {
     *      version Version,
     *      SignerIdentifier sid,
     *      digestAlgorithm DigestAlgorithmIdentifier,
     *      authenticatedAttributes [0] IMPLICIT Attributes OPTIONAL,
     *      digestEncryptionAlgorithm DigestEncryptionAlgorithmIdentifier,
     *      encryptedDigest EncryptedDigest,
     *      unauthenticatedAttributes [1] IMPLICIT Attributes OPTIONAL
     *  }
     *
     *  EncryptedDigest ::= OCTET STRING
     *
     *  DigestAlgorithmIdentifier ::= AlgorithmIdentifier
     *
     *  DigestEncryptionAlgorithmIdentifier ::= AlgorithmIdentifier
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(version);
        v.add(sid);
        v.add(digAlgorithm);

        if (authenticatedAttributes != null)
        {
            v.add(new DERTaggedObject(false, 0, authenticatedAttributes));
        }

        v.add(digEncryptionAlgorithm);
        v.add(encryptedDigest);

        if (unauthenticatedAttributes != null)
        {
            v.add(new DERTaggedObject(false, 1, unauthenticatedAttributes));
        }

        return new DERSequence(v);
    }
}
