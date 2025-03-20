/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.modes.gcm;

import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

public class BasicGCMMultiplier implements GCMMultiplier
{
    private byte[] H;

    public void init(byte[] H)
    {
        this.H = Arrays.clone(H);
    }

    public void multiplyH(byte[] x)
    {
        byte[] z = new byte[16];

        for (int i = 0; i < 16; ++i)
        {
            byte h = H[i];
            for (int j = 7; j >= 0; --j)
            {
                if ((h & (1 << j)) != 0)
                {
                    GCMUtil.xor(z, x);
                }

                boolean lsb = (x[15] & 1) != 0;
                GCMUtil.shiftRight(x);
                if (lsb)
                {
                    // R = new byte[]{ 0xe1, ... };
//                    GCMUtil.xor(v, R);
                    x[0] ^= (byte)0xe1;
                }
            }
        }

        System.arraycopy(z, 0, x, 0, 16);        
    }
}
