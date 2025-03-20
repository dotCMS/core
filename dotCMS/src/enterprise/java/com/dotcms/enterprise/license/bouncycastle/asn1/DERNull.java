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
public class DERNull
    extends ASN1Null
{
    public static final DERNull INSTANCE = new DERNull();

    byte[]  zeroBytes = new byte[0];

    public DERNull()
    {
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        out.writeEncoded(NULL, zeroBytes);
    }
}
