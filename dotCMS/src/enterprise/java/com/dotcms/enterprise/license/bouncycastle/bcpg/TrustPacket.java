/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Basic type for a trust packet
 */
public class TrustPacket 
    extends ContainedPacket
{    
    byte[]    levelAndTrustAmount;
    
    public TrustPacket(
        BCPGInputStream  in)
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        int                      ch;
        
        while ((ch = in.read()) >= 0)
        {
            bOut.write(ch);
        }
        
        levelAndTrustAmount = bOut.toByteArray();
    }
    
    public TrustPacket(
        int    trustCode)
    {
        this.levelAndTrustAmount = new byte[1];
        
        this.levelAndTrustAmount[0] = (byte)trustCode;
    }

    public byte[] getLevelAndTrustAmount()
    {
        return levelAndTrustAmount;
    }

    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(TRUST, levelAndTrustAmount, true);
    }
}
