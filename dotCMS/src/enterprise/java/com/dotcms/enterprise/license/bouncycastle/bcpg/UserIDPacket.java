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

import com.dotcms.enterprise.license.bouncycastle.util.Strings;

/**
 * Basic type for a user ID packet.
 */
public class UserIDPacket 
    extends ContainedPacket
{    
    private byte[]    idData;
    
    public UserIDPacket(
        BCPGInputStream  in)
        throws IOException
    {
        idData = new byte[in.available()];
        in.readFully(idData);
    }
    
    public UserIDPacket(
        String    id)
    {
        this.idData = Strings.toUTF8ByteArray(id);
    }
    
    public String getID()
    {
        return Strings.fromUTF8ByteArray(idData);
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(USER_ID, idData, true);
    }
}
