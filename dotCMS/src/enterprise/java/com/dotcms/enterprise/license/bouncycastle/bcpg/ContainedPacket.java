/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;

/**
 * Basic type for a PGP packet.
 */
public abstract class ContainedPacket 
    extends Packet
{
    public byte[] getEncoded() 
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        BCPGOutputStream         pOut = new BCPGOutputStream(bOut);
        
        pOut.writePacket(this);
        
        return bOut.toByteArray();
    }
    
    public abstract void encode(
        BCPGOutputStream    pOut)
        throws IOException;
}
