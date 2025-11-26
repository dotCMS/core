/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;


public class ElGamalKeyParameters
    extends AsymmetricKeyParameter
{
    private ElGamalParameters    params;

    protected ElGamalKeyParameters(
        boolean         isPrivate,
        ElGamalParameters    params)
    {
        super(isPrivate);

        this.params = params;
    }   

    public ElGamalParameters getParameters()
    {
        return params;
    }

    public int hashCode()
    {
        return (params != null) ? params.hashCode() : 0;
    }

    public boolean equals(
        Object  obj)
    {
        if (!(obj instanceof ElGamalKeyParameters))
        {
            return false;
        }

        ElGamalKeyParameters    dhKey = (ElGamalKeyParameters)obj;

        if (params == null)
        {
            return dhKey.getParameters() == null;
        }
        else
        { 
            return params.equals(dhKey.getParameters());
        }
    }
}
