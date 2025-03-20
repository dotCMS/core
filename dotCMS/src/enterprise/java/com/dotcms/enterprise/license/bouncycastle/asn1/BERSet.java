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

public class BERSet
    extends DERSet
{
    /**
     * create an empty sequence
     */
    public BERSet()
    {
    }

    /**
     * create a set containing one object
     */
    public BERSet(
        DEREncodable    obj)
    {
        super(obj);
    }

    /**
     * @param v - a vector of objects making up the set.
     */
    public BERSet(
        DEREncodableVector   v)
    {
        super(v, false);
    }

    /**
     * @param v - a vector of objects making up the set.
     */
    BERSet(
        DEREncodableVector   v,
        boolean              needsSorting)
    {
        super(v, needsSorting);
    }

    /*
     */
    void encode(
        DEROutputStream out)
        throws IOException
    {
        if (out instanceof ASN1OutputStream || out instanceof BEROutputStream)
        {
            out.write(SET | CONSTRUCTED);
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
