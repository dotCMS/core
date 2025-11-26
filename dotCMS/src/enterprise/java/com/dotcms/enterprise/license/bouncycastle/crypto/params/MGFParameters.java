/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import com.dotcms.enterprise.license.bouncycastle.crypto.DerivationParameters;

/**
 * parameters for mask derivation functions.
 */
public class MGFParameters
    implements DerivationParameters
{
    byte[]  seed;

    public MGFParameters(
        byte[]  seed)
    {
        this(seed, 0, seed.length);
    }

    public MGFParameters(
        byte[]  seed,
        int     off,
        int     len)
    {
        this.seed = new byte[len];
        System.arraycopy(seed, off, this.seed, 0, len);
    }

    public byte[] getSeed()
    {
        return seed;
    }
}
