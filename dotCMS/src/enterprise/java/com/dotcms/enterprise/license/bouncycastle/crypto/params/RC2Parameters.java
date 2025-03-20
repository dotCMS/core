/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;

public class RC2Parameters
    implements CipherParameters
{
    private byte[]  key;
    private int     bits;

    public RC2Parameters(
        byte[]  key)
    {
        this(key, (key.length > 128) ? 1024 : (key.length * 8));
    }

    public RC2Parameters(
        byte[]  key,
        int     bits)
    {
        this.key = new byte[key.length];
        this.bits = bits;

        System.arraycopy(key, 0, this.key, 0, key.length);
    }

    public byte[] getKey()
    {
        return key;
    }

    public int getEffectiveKeyBits()
    {
        return bits;
    }
}
