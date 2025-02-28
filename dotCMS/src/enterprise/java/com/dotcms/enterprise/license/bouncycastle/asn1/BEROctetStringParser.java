/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import com.dotcms.enterprise.license.bouncycastle.util.io.Streams;

import java.io.InputStream;
import java.io.IOException;

public class BEROctetStringParser
    implements ASN1OctetStringParser
{
    private ASN1StreamParser _parser;

    BEROctetStringParser(
        ASN1StreamParser parser)
    {
        _parser = parser;
    }

    /**
     * @deprecated will be removed
     */
    protected BEROctetStringParser(
        ASN1ObjectParser parser)
    {
        _parser = parser._aIn;
    }

    public InputStream getOctetStream()
    {
        return new ConstructedOctetStream(_parser);
    }

    public DERObject getDERObject()
    {
        try
        {
            return new BERConstructedOctetString(Streams.readAll(getOctetStream()));
        }
        catch (IOException e)
        {
            throw new ASN1ParsingException("IOException converting stream to byte array: " + e.getMessage(), e);
        }
    }
}
