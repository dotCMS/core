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
 * basic packet for an experimental packet.
 */
public class ExperimentalPacket 
    extends ContainedPacket implements PublicKeyAlgorithmTags
{
    private int    tag;
    private byte[] contents;
    
    /**
     * 
     * @param in
     * @throws IOException
     */
    ExperimentalPacket(
        int                tag,
        BCPGInputStream    in)
        throws IOException
    {
        this.tag = tag;
        
        if (in.available() != 0)
        {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream(in.available());
            
            int b;
            while ((b = in.read()) >= 0) 
            {
                 bOut.write(b);
            }
            
            contents = bOut.toByteArray();
        }
        else
        {
            contents = new byte[0];
        }
    }
    
    public int getTag()
    {
        return tag;
    }
    
    public byte[] getContents()
    {
        byte[]    tmp = new byte[contents.length];
        
        System.arraycopy(contents, 0, tmp, 0, tmp.length);
        
        return tmp;
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(tag, contents, true);
    }
}
