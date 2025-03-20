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

public class DERSequenceParser
    implements ASN1SequenceParser
{
    private ASN1StreamParser _parser;

    DERSequenceParser(ASN1StreamParser parser)
    {
        this._parser = parser;
    }

    public DEREncodable readObject()
        throws IOException
    {
        return _parser.readObject();
    }

    public DERObject getDERObject()
    {
        try
        {
            return new DERSequence(_parser.readVector());
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage(),e);
        }
    }
}
