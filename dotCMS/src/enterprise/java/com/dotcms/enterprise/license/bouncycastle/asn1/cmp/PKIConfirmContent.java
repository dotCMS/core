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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Null;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;

public class PKIConfirmContent
    extends ASN1Encodable
{
    private ASN1Null val;

    private PKIConfirmContent(ASN1Null val)
    {
        this.val = val;
    }

    public static PKIConfirmContent getInstance(Object o)
    {
        if (o instanceof PKIConfirmContent)
        {
            return (PKIConfirmContent)o;
        }

        if (o instanceof ASN1Null)
        {
            return new PKIConfirmContent((ASN1Null)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    /**
     * <pre>
     * PKIConfirmContent ::= NULL
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return val;
    }
}
