/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cmp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class CAKeyUpdAnnContent
    extends ASN1Encodable
{
    private CMPCertificate oldWithNew;
    private CMPCertificate newWithOld;
    private CMPCertificate newWithNew;

    private CAKeyUpdAnnContent(ASN1Sequence seq)
    {
        oldWithNew = CMPCertificate.getInstance(seq.getObjectAt(0));
        newWithOld = CMPCertificate.getInstance(seq.getObjectAt(1));
        newWithNew = CMPCertificate.getInstance(seq.getObjectAt(2));
    }

    public static CAKeyUpdAnnContent getInstance(Object o)
    {
        if (o instanceof CAKeyUpdAnnContent)
        {
            return (CAKeyUpdAnnContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new CAKeyUpdAnnContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public CMPCertificate getOldWithNew()
    {
        return oldWithNew;
    }

    public CMPCertificate getNewWithOld()
    {
        return newWithOld;
    }

    public CMPCertificate getNewWithNew()
    {
        return newWithNew;
    }

    /**
     * <pre>
     * CAKeyUpdAnnContent ::= SEQUENCE {
     *                             oldWithNew   CMPCertificate, -- old pub signed with new priv
     *                             newWithOld   CMPCertificate, -- new pub signed with old priv
     *                             newWithNew   CMPCertificate  -- new pub signed with new priv
     *  }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(oldWithNew);
        v.add(newWithOld);
        v.add(newWithNew);

        return new DERSequence(v);
    }
}
