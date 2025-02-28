/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.util.Vector;

/**
 * a general class for building up a vector of DER encodable objects -
 * this will eventually be superceded by ASN1EncodableVector so you should
 * use that class in preference.
 */
public class DEREncodableVector
{
    Vector v = new Vector();

    /**
     * @deprecated use ASN1EncodableVector instead.
     */
    public DEREncodableVector()
    {

    }
    
    public void add(
        DEREncodable   obj)
    {
        v.addElement(obj);
    }

    public DEREncodable get(
        int i)
    {
        return (DEREncodable)v.elementAt(i);
    }

    public int size()
    {
        return v.size();
    }
}
