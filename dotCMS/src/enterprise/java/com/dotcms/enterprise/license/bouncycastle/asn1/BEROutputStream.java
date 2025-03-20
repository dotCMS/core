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
import java.io.OutputStream;

public class BEROutputStream
    extends DEROutputStream
{
    public BEROutputStream(
        OutputStream    os)
    {
        super(os);
    }

    public void writeObject(
        Object    obj)
        throws IOException
    {
        if (obj == null)
        {
            writeNull();
        }
        else if (obj instanceof DERObject)
        {
            ((DERObject)obj).encode(this);
        }
        else if (obj instanceof DEREncodable)
        {
            ((DEREncodable)obj).getDERObject().encode(this);
        }
        else
        {
            throw new IOException("object not BEREncodable");
        }
    }
}
