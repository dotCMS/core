/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;

import java.math.BigInteger;

/**
 * The CRLNumber object.
 * <pre>
 * CRLNumber::= INTEGER(0..MAX)
 * </pre>
 */
public class CRLNumber
    extends DERInteger
{

    public CRLNumber(
        BigInteger number)
    {
        super(number);
    }

    public BigInteger getCRLNumber()
    {
        return getPositiveValue();
    }

    public String toString()
    {
        return "CRLNumber: " + getCRLNumber();
    }
}
