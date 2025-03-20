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

public class BERApplicationSpecificParser
    implements ASN1ApplicationSpecificParser
{
    private final int tag;
    private final ASN1StreamParser parser;

    BERApplicationSpecificParser(int tag, ASN1StreamParser parser)
    {
        this.tag = tag;
        this.parser = parser;
    }

    public DEREncodable readObject()
        throws IOException
    {
        return parser.readObject();
    }

    public DERObject getDERObject()
    {
        try
        {
            return new BERApplicationSpecific(tag, parser.readVector());
        }
        catch (IOException e)
        {
            throw new ASN1ParsingException(e.getMessage(), e);
        }
    }
}
