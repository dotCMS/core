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

public class DEROctetString
    extends ASN1OctetString
{
    /**
     * @param string the octets making up the octet string.
     */
    public DEROctetString(
        byte[]  string)
    {
        super(string);
    }

    public DEROctetString(
        DEREncodable  obj)
    {
        super(obj);
    }

    void encode(
        DEROutputStream out)
        throws IOException
    {
        out.writeEncoded(OCTET_STRING, string);
    }

    static void encode(
        DEROutputStream derOut,
        byte[]          bytes)
        throws IOException
    {
        derOut.writeEncoded(DERTags.OCTET_STRING, bytes);
    }
}
