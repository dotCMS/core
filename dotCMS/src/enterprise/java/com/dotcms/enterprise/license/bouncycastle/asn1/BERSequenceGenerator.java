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

public class BERSequenceGenerator
    extends BERGenerator
{
    public BERSequenceGenerator(
        OutputStream out) 
        throws IOException
    {
        super(out);

        writeBERHeader(DERTags.CONSTRUCTED | DERTags.SEQUENCE);
    }

    public BERSequenceGenerator(
        OutputStream out,
        int tagNo,
        boolean isExplicit) 
        throws IOException
    {
        super(out, tagNo, isExplicit);
        
        writeBERHeader(DERTags.CONSTRUCTED | DERTags.SEQUENCE);
    }

    public void addObject(
        DEREncodable object)
        throws IOException
    {
        object.getDERObject().encode(new BEROutputStream(_out));
    }
    
    public void close() 
        throws IOException
    {
        writeBEREnd();
    }
}
