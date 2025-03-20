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
import java.util.Vector;

/**
 * Basic type for a user attribute packet.
 */
public class UserAttributePacket 
    extends ContainedPacket
{    
    private UserAttributeSubpacket[]    subpackets;
    
    public UserAttributePacket(
        BCPGInputStream  in)
        throws IOException
    {
        UserAttributeSubpacketInputStream     sIn = new UserAttributeSubpacketInputStream(in);
        UserAttributeSubpacket                sub;
                                        
        Vector    v= new Vector();
        while ((sub = sIn.readPacket()) != null)
        {
            v.addElement(sub);
        }
        
        subpackets = new UserAttributeSubpacket[v.size()];
            
        for (int i = 0; i != subpackets.length; i++)
        {
            subpackets[i] = (UserAttributeSubpacket)v.elementAt(i);
        }
    }
    
    public UserAttributePacket(
        UserAttributeSubpacket[]    subpackets)
    {
        this.subpackets = subpackets;
    }
    
    public UserAttributeSubpacket[] getSubpackets()
    {
        return subpackets;
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        for (int i = 0; i != subpackets.length; i++)
        {
            subpackets[i].encode(bOut);
        }

        out.writePacket(USER_ATTRIBUTE, bOut.toByteArray(), false);
    }
}
