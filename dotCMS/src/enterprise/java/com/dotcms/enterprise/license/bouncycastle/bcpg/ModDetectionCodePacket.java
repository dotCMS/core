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
 * basic packet for a modification detection code packet.
 */
public class ModDetectionCodePacket 
    extends ContainedPacket
{    
    private byte[]    digest;
    
    ModDetectionCodePacket(
        BCPGInputStream in)
        throws IOException
    {    
        this.digest = new byte[20];
        in.readFully(this.digest);
    }
    
    public ModDetectionCodePacket(
        byte[]    digest)
        throws IOException
    {    
        this.digest = new byte[digest.length];
        
        System.arraycopy(digest, 0, this.digest, 0, this.digest.length);
    }
    
    public byte[] getDigest()
    {
        byte[] tmp = new byte[digest.length];
        
        System.arraycopy(digest, 0, tmp, 0, tmp.length);
        
        return tmp;
    }
    
    public void encode(
        BCPGOutputStream    out) 
        throws IOException
    {
        out.writePacket(MOD_DETECTION_CODE, digest, false);
    }
}
