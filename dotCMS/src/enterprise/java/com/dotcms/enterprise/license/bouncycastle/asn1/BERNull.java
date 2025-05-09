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
 * A BER NULL object.
 */
public class BERNull
    extends DERNull
{
    public static final BERNull INSTANCE = new BERNull();

    public BERNull()
    {
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        if (out instanceof ASN1OutputStream || out instanceof BEROutputStream)
        {
            out.write(NULL);
        }
        else
        {
            super.encode(out);
        }
    }
}
