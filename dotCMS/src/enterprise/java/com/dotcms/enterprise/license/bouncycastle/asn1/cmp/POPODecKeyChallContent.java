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

public class POPODecKeyChallContent
    extends ASN1Encodable
{
    private ASN1Sequence content;

    private POPODecKeyChallContent(ASN1Sequence seq)
    {
        content = seq;
    }

    public static POPODecKeyChallContent getInstance(Object o)
    {
        if (o instanceof POPODecKeyChallContent)
        {
            return (POPODecKeyChallContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new POPODecKeyChallContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public Challenge[] toChallengeArray()
    {
        Challenge[] result = new Challenge[content.size()];

        for (int i = 0; i != result.length; i++)
        {
            result[i] = Challenge.getInstance(content.getObjectAt(i));
        }

        return result;
    }

    /**
     * <pre>
     * POPODecKeyChallContent ::= SEQUENCE OF Challenge
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return content;
    }
}
