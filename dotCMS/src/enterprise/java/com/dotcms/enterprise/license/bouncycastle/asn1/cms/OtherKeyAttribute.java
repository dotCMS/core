/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cms;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class OtherKeyAttribute
    extends ASN1Encodable
{
    private DERObjectIdentifier keyAttrId;
    private DEREncodable        keyAttr;

    /**
     * return an OtherKeyAttribute object from the given object.
     *
     * @param o the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static OtherKeyAttribute getInstance(
        Object o)
    {
        if (o == null || o instanceof OtherKeyAttribute)
        {
            return (OtherKeyAttribute)o;
        }
        
        if (o instanceof ASN1Sequence)
        {
            return new OtherKeyAttribute((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("unknown object in factory: " + o.getClass().getName());
    }
    
    public OtherKeyAttribute(
        ASN1Sequence seq)
    {
        keyAttrId = (DERObjectIdentifier)seq.getObjectAt(0);
        keyAttr = seq.getObjectAt(1);
    }

    public OtherKeyAttribute(
        DERObjectIdentifier keyAttrId,
        DEREncodable        keyAttr)
    {
        this.keyAttrId = keyAttrId;
        this.keyAttr = keyAttr;
    }

    public DERObjectIdentifier getKeyAttrId()
    {
        return keyAttrId;
    }
    
    public DEREncodable getKeyAttr()
    {
        return keyAttr;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * OtherKeyAttribute ::= SEQUENCE {
     *     keyAttrId OBJECT IDENTIFIER,
     *     keyAttr ANY DEFINED BY keyAttrId OPTIONAL
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(keyAttrId);
        v.add(keyAttr);

        return new DERSequence(v);
    }
}
