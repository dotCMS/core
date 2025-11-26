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

/**
 * Basic type for a marker packet
 */
public class MarkerPacket 
    extends ContainedPacket
{    
    // "PGP"
        
    byte[]    marker = { (byte)0x50, (byte)0x47, (byte)0x50 };
    
    public MarkerPacket(
        BCPGInputStream  in)
        throws IOException
    {
         in.readFully(marker);
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(MARKER, marker, true);
    }
}
