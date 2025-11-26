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

public class ParametersWithSBox
    implements CipherParameters
{
    private CipherParameters  parameters;
    private byte[]            sBox;

    public ParametersWithSBox(
        CipherParameters parameters,
        byte[]           sBox)
    {
        this.parameters = parameters;
        this.sBox = sBox;
    }

    public byte[] getSBox()
    {
        return sBox;
    }

    public CipherParameters getParameters()
    {
        return parameters;
    }
}
