/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.crmf;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;

public class Controls
    extends ASN1Encodable
{
    private ASN1Sequence content;

    private Controls(ASN1Sequence seq)
    {
        content = seq;
    }

    public static Controls getInstance(Object o)
    {
        if (o instanceof Controls)
        {
            return (Controls)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new Controls((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public AttributeTypeAndValue[] toAttributeTypeAndValueArray()
    {
        AttributeTypeAndValue[] result = new AttributeTypeAndValue[content.size()];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = AttributeTypeAndValue.getInstance(content.getObjectAt(i));
        }

        return result;
    }

    /**
     * <pre>
     * Controls  ::= SEQUENCE SIZE(1..MAX) OF AttributeTypeAndValue
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return content;
    }
}
