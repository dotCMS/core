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

package com.dotcms.enterprise.license.bouncycastle.asn1.cms.ecc;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.cms.OriginatorPublicKey;

public class MQVuserKeyingMaterial
    extends ASN1Encodable
{
    private OriginatorPublicKey ephemeralPublicKey;
    private ASN1OctetString addedukm;

    public MQVuserKeyingMaterial(
        OriginatorPublicKey ephemeralPublicKey,
        ASN1OctetString addedukm)
    {
        // TODO Check ephemeralPublicKey not null
        
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.addedukm = addedukm;
    }

    private MQVuserKeyingMaterial(
        ASN1Sequence seq)
    {
        // TODO Check seq has either 1 or 2 elements

        this.ephemeralPublicKey = OriginatorPublicKey.getInstance(
            seq.getObjectAt(0));

        if (seq.size() > 1)
        {
            this.addedukm = ASN1OctetString.getInstance(
                (ASN1TaggedObject)seq.getObjectAt(1), true);
        }
    }

    /**
     * return an MQVuserKeyingMaterial object from a tagged object.
     *
     * @param obj      the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *                 tagged false otherwise.
     * @throws IllegalArgumentException if the object held by the
     *                                  tagged object cannot be converted.
     */
    public static MQVuserKeyingMaterial getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * return an MQVuserKeyingMaterial object from the given object.
     *
     * @param obj the object we want converted.
     * @throws IllegalArgumentException if the object cannot be converted.
     */
    public static MQVuserKeyingMaterial getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof MQVuserKeyingMaterial)
        {
            return (MQVuserKeyingMaterial)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new MQVuserKeyingMaterial((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid MQVuserKeyingMaterial: " + obj.getClass().getName());
    }

    public OriginatorPublicKey getEphemeralPublicKey()
    {
        return ephemeralPublicKey;
    }

    public ASN1OctetString getAddedukm()
    {
        return addedukm;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * MQVuserKeyingMaterial ::= SEQUENCE {
     *   ephemeralPublicKey OriginatorPublicKey,
     *   addedukm [0] EXPLICIT UserKeyingMaterial OPTIONAL  }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(ephemeralPublicKey);

        if (addedukm != null)
        {
            v.add(new DERTaggedObject(true, 0, addedukm));
        }

        return new DERSequence(v);
    }
}
