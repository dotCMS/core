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

public class ProtectedPart
    extends ASN1Encodable
{
    private PKIHeader header;
    private PKIBody body;

    private ProtectedPart(ASN1Sequence seq)
    {
        header = PKIHeader.getInstance(seq.getObjectAt(0));
        body = PKIBody.getInstance(seq.getObjectAt(1));
    }

    public static ProtectedPart getInstance(Object o)
    {
        if (o instanceof ProtectedPart)
        {
            return (ProtectedPart)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new ProtectedPart((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public PKIHeader getHeader()
    {
        return header;
    }

    public PKIBody getBody()
    {
        return body;
    }

    /**
     * <pre>
     * ProtectedPart ::= SEQUENCE {
     *                    header    PKIHeader,
     *                    body      PKIBody
     * }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(header);
        v.add(body);

        return new DERSequence(v);
    }
}
