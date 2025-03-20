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
import java.util.Enumeration;

public class BERSequence
    extends DERSequence
{
    /**
     * create an empty sequence
     */
    public BERSequence()
    {
    }

    /**
     * create a sequence containing one object
     */
    public BERSequence(
        DEREncodable    obj)
    {
        super(obj);
    }

    /**
     * create a sequence containing a vector of objects.
     */
    public BERSequence(
        DEREncodableVector   v)
    {
        super(v);
    }

    /*
     */
    void encode(
        DEROutputStream out)
        throws IOException
    {
        if (out instanceof ASN1OutputStream || out instanceof BEROutputStream)
        {
            out.write(SEQUENCE | CONSTRUCTED);
            out.write(0x80);
            
            Enumeration e = getObjects();
            while (e.hasMoreElements())
            {
                out.writeObject(e.nextElement());
            }
        
            out.write(0x00);
            out.write(0x00);
        }
        else
        {
            super.encode(out);
        }
    }
}
