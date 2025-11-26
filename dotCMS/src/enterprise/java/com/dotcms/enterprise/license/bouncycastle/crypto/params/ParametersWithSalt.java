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

/**
 * Cipher parameters with a fixed salt value associated with them.
 */
public class ParametersWithSalt
    implements CipherParameters
{
    private byte[]              salt;
    private CipherParameters    parameters;

    public ParametersWithSalt(
        CipherParameters    parameters,
        byte[]              salt)
    {
        this(parameters, salt, 0, salt.length);
    }

    public ParametersWithSalt(
        CipherParameters    parameters,
        byte[]              salt,
        int                 saltOff,
        int                 saltLen)
    {
        this.salt = new byte[saltLen];
        this.parameters = parameters;

        System.arraycopy(salt, saltOff, this.salt, 0, saltLen);
    }

    public byte[] getSalt()
    {
        return salt;
    }

    public CipherParameters getParameters()
    {
        return parameters;
    }
}
