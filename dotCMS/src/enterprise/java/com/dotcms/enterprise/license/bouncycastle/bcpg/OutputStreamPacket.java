/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.IOException;

public abstract class OutputStreamPacket
{
    protected BCPGOutputStream    out;
    
    public OutputStreamPacket(
        BCPGOutputStream    out)
    {
        this.out = out;
    }
    
    public abstract BCPGOutputStream open() throws IOException;
    
    public abstract void close() throws IOException;
}
