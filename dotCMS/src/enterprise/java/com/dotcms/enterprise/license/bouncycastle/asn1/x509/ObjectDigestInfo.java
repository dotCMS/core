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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBitString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREnumerated;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * ObjectDigestInfo ASN.1 structure used in v2 attribute certificates.
 * 
 * <pre>
 *  
 *    ObjectDigestInfo ::= SEQUENCE {
 *         digestedObjectType  ENUMERATED {
 *                 publicKey            (0),
 *                 publicKeyCert        (1),
 *                 otherObjectTypes     (2) },
 *                         -- otherObjectTypes MUST NOT
 *                         -- be used in this profile
 *         otherObjectTypeID   OBJECT IDENTIFIER OPTIONAL,
 *         digestAlgorithm     AlgorithmIdentifier,
 *         objectDigest        BIT STRING
 *    }
 *   
 * </pre>
 * 
 */
public class ObjectDigestInfo
    extends ASN1Encodable
{
    /**
     * The public key is hashed.
     */
    public final static int publicKey = 0;

    /**
     * The public key certificate is hashed.
     */
    public final static int publicKeyCert = 1;

    /**
     * An other object is hashed.
     */
    public final static int otherObjectDigest = 2;

    DEREnumerated digestedObjectType;

    DERObjectIdentifier otherObjectTypeID;

    AlgorithmIdentifier digestAlgorithm;

    DERBitString objectDigest;

    public static ObjectDigestInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof ObjectDigestInfo)
        {
            return (ObjectDigestInfo)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new ObjectDigestInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    public static ObjectDigestInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * Constructor from given details.
     * <p>
     * If <code>digestedObjectType</code> is not {@link #publicKeyCert} or
     * {@link #publicKey} <code>otherObjectTypeID</code> must be given,
     * otherwise it is ignored.
     * 
     * @param digestedObjectType The digest object type.
     * @param otherObjectTypeID The object type ID for
     *            <code>otherObjectDigest</code>.
     * @param digestAlgorithm The algorithm identifier for the hash.
     * @param objectDigest The hash value.
     */
    public ObjectDigestInfo(
        int digestedObjectType,
        String otherObjectTypeID,
        AlgorithmIdentifier digestAlgorithm,
        byte[] objectDigest)
    {
        this.digestedObjectType = new DEREnumerated(digestedObjectType);
        if (digestedObjectType == otherObjectDigest)
        {
            this.otherObjectTypeID = new DERObjectIdentifier(otherObjectTypeID);
        }

        this.digestAlgorithm = digestAlgorithm; 

        this.objectDigest = new DERBitString(objectDigest);
    }

    private ObjectDigestInfo(
        ASN1Sequence seq)
    {
        if (seq.size() > 4 || seq.size() < 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        digestedObjectType = DEREnumerated.getInstance(seq.getObjectAt(0));

        int offset = 0;

        if (seq.size() == 4)
        {
            otherObjectTypeID = DERObjectIdentifier.getInstance(seq.getObjectAt(1));
            offset++;
        }

        digestAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(1 + offset));

        objectDigest = DERBitString.getInstance(seq.getObjectAt(2 + offset));
    }

    public DEREnumerated getDigestedObjectType()
    {
        return digestedObjectType;
    }

    public DERObjectIdentifier getOtherObjectTypeID()
    {
        return otherObjectTypeID;
    }

    public AlgorithmIdentifier getDigestAlgorithm()
    {
        return digestAlgorithm;
    }

    public DERBitString getObjectDigest()
    {
        return objectDigest;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * <pre>
     *  
     *    ObjectDigestInfo ::= SEQUENCE {
     *         digestedObjectType  ENUMERATED {
     *                 publicKey            (0),
     *                 publicKeyCert        (1),
     *                 otherObjectTypes     (2) },
     *                         -- otherObjectTypes MUST NOT
     *                         -- be used in this profile
     *         otherObjectTypeID   OBJECT IDENTIFIER OPTIONAL,
     *         digestAlgorithm     AlgorithmIdentifier,
     *         objectDigest        BIT STRING
     *    }
     *   
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(digestedObjectType);

        if (otherObjectTypeID != null)
        {
            v.add(otherObjectTypeID);
        }

        v.add(digestAlgorithm);
        v.add(objectDigest);

        return new DERSequence(v);
    }
}
