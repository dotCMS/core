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
 * base class for a PGP object.
 */
public abstract class BCPGObject 
{
    public byte[] getEncoded() 
        throws IOException
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        BCPGOutputStream         pOut = new BCPGOutputStream(bOut);
        
        pOut.writeObject(this);
        
        return bOut.toByteArray();
    }
    
    public abstract void encode(BCPGOutputStream out)
        throws IOException;
}
