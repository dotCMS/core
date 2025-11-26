/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

/**
 *
 */
public class InputStreamPacket
    extends Packet
{
    private BCPGInputStream        in;
    
    public InputStreamPacket(
        BCPGInputStream  in)
    {
        this.in = in;
    }
    
    /**
     * Note: you can only read from this once...
     *
     * @return the InputStream
     */
    public BCPGInputStream getInputStream()
    {
        return in;
    }
}
