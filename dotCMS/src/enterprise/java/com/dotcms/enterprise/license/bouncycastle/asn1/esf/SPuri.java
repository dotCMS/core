/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.esf;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;

public class SPuri
{
    private DERIA5String uri;

    public static SPuri getInstance(
        Object obj)
    {
        if (obj instanceof SPuri)
        {
            return (SPuri) obj;
        }
        else if (obj instanceof DERIA5String)
        {
            return new SPuri((DERIA5String) obj);
        }

        throw new IllegalArgumentException(
                "unknown object in 'SPuri' factory: "
                        + obj.getClass().getName() + ".");
    }

    public SPuri(
        DERIA5String uri)
    {
        this.uri = uri;
    }

    public DERIA5String getUri()
    {
        return uri;
    }

    /**
     * <pre>
     * SPuri ::= IA5String
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return uri.getDERObject();
    }
}
