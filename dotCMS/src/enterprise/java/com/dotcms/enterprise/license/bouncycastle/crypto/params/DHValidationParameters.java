/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

public class DHValidationParameters
{
    private byte[]  seed;
    private int     counter;

    public DHValidationParameters(
        byte[]  seed,
        int     counter)
    {
        this.seed = seed;
        this.counter = counter;
    }

    public int getCounter()
    {
        return counter;
    }

    public byte[] getSeed()
    {
        return seed;
    }

    public boolean equals(
        Object o)
    {
        if (!(o instanceof DHValidationParameters))
        {
            return false;
        }

        DHValidationParameters  other = (DHValidationParameters)o;

        if (other.counter != this.counter)
        {
            return false;
        }

        return Arrays.areEqual(this.seed, other.seed);
    }

    public int hashCode()
    {
        return counter ^ Arrays.hashCode(seed);
    }
}
