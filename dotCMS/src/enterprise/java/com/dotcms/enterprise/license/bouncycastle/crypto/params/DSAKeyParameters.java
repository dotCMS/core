/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

public class DSAKeyParameters
    extends AsymmetricKeyParameter
{
    private DSAParameters    params;

    public DSAKeyParameters(
        boolean         isPrivate,
        DSAParameters   params)
    {
        super(isPrivate);

        this.params = params;
    }   

    public DSAParameters getParameters()
    {
        return params;
    }
}
