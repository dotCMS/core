/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.math.ec;

import java.math.BigInteger;

/**
 * Interface for classes encapsulating a point multiplication algorithm
 * for <code>ECPoint</code>s.
 */
interface ECMultiplier
{
    /**
     * Multiplies the <code>ECPoint p</code> by <code>k</code>, i.e.
     * <code>p</code> is added <code>k</code> times to itself.
     * @param p The <code>ECPoint</code> to be multiplied.
     * @param k The factor by which <code>p</code> i multiplied.
     * @return <code>p</code> multiplied by <code>k</code>.
     */
    ECPoint multiply(ECPoint p, BigInteger k, PreCompInfo preCompInfo);
}
