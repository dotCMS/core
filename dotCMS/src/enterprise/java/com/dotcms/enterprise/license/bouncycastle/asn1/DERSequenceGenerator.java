/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DERSequenceGenerator
    extends DERGenerator
{
    private final ByteArrayOutputStream _bOut = new ByteArrayOutputStream();

    public DERSequenceGenerator(
        OutputStream out)
        throws IOException
    {
        super(out);
    }

    public DERSequenceGenerator(
        OutputStream out,
        int          tagNo,
        boolean      isExplicit)
        throws IOException
    {
        super(out, tagNo, isExplicit);
    }

    public void addObject(
        DEREncodable object) 
        throws IOException
    {
        object.getDERObject().encode(new DEROutputStream(_bOut));
    }
    
    public OutputStream getRawOutputStream()
    {
        return _bOut;
    }
    
    public void close() 
        throws IOException
    {
        writeDEREncoded(DERTags.CONSTRUCTED | DERTags.SEQUENCE, _bOut.toByteArray());
    }
}
