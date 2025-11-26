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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;

public class GenMsgContent
    extends ASN1Encodable
{
    private ASN1Sequence content;

    private GenMsgContent(ASN1Sequence seq)
    {
        content = seq;
    }

    public static GenMsgContent getInstance(Object o)
    {
        if (o instanceof GenMsgContent)
        {
            return (GenMsgContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new GenMsgContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public InfoTypeAndValue[] toInfoTypeAndValueArray()
    {
        InfoTypeAndValue[] result = new InfoTypeAndValue[content.size()];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = InfoTypeAndValue.getInstance(content.getObjectAt(i));
        }

        return result;
    }

    /**
     * <pre>
     * GenMsgContent ::= SEQUENCE OF InfoTypeAndValue
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return content;
    }
}
