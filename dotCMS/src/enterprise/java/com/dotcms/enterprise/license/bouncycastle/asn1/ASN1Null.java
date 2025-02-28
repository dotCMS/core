/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.IOException;

/**
 * A NULL object.
 */
public abstract class ASN1Null
    extends ASN1Object
{
    public ASN1Null()
    {
    }

    public int hashCode()
    {
        return -1;
    }

    boolean asn1Equals(
        DERObject o)
    {
        if (!(o instanceof ASN1Null))
        {
            return false;
        }
        
        return true;
    }

    abstract void encode(DEROutputStream out)
        throws IOException;

    public String toString()
    {
         return "NULL";
    }
}
