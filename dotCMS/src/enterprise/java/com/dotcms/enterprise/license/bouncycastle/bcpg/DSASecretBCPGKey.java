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
import java.math.BigInteger;

/**
 * base class for a DSA Secret Key.
 */
public class DSASecretBCPGKey 
    extends BCPGObject implements BCPGKey 
{
    MPInteger    x;
    
    /**
     * 
     * @param in
     * @throws IOException
     */
    public DSASecretBCPGKey(
        BCPGInputStream    in)
        throws IOException
    {
        this.x = new MPInteger(in);
    }

    /**
     * 
     * @param x
     */
    public DSASecretBCPGKey(
        BigInteger    x)
    {
        this.x = new MPInteger(x);
    }
    
    /**
     *  return "PGP"
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.bcpg.BCPGKey#getFormat()
     */
    public String getFormat() 
    {
        return "PGP";
    }

    /**
     * return the standard PGP encoding of the key.
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.bcpg.BCPGKey#getEncoded()
     */
    public byte[] getEncoded() 
    {
        try
        { 
            ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
            BCPGOutputStream       pgpOut = new BCPGOutputStream(bOut);
        
            pgpOut.writeObject(this);
        
            return bOut.toByteArray();
        }
        catch (IOException e)
        {
            return null;
        }
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writeObject(x);
    }
    
    /**
     * @return x
     */
    public BigInteger getX()
    {
        return x.getValue();
    }
}
