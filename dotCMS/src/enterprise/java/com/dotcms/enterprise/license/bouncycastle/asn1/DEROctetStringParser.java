/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.InputStream;
import java.io.IOException;

public class DEROctetStringParser
    implements ASN1OctetStringParser
{
    private DefiniteLengthInputStream stream;

    DEROctetStringParser(
        DefiniteLengthInputStream stream)
    {
        this.stream = stream;
    }

    public InputStream getOctetStream()
    {
        return stream;
    }

    public DERObject getDERObject()
    {
        try
        {
            return new DEROctetString(stream.toByteArray());
        }
        catch (IOException e)
        {
            throw new ASN1ParsingException("IOException converting stream to byte array: " + e.getMessage(), e);
        }
    }
}
