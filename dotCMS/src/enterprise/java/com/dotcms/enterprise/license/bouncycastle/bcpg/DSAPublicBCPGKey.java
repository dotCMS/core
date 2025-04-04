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
 * base class for a DSA Public Key.
 */
public class DSAPublicBCPGKey 
    extends BCPGObject implements BCPGKey 
{
    MPInteger    p;
    MPInteger    q;
    MPInteger    g;
    MPInteger    y;
    
    /**
     * @param in the stream to read the packet from.
     */
    public DSAPublicBCPGKey(
        BCPGInputStream    in)
        throws IOException
    {
        this.p = new MPInteger(in);
        this.q = new MPInteger(in);
        this.g = new MPInteger(in);
        this.y = new MPInteger(in);
    }

    public DSAPublicBCPGKey(
        BigInteger    p,
        BigInteger    q,
        BigInteger    g,
        BigInteger    y)
    {
        this.p = new MPInteger(p);
        this.q = new MPInteger(q);
        this.g = new MPInteger(g);
        this.y = new MPInteger(y);
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
            ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
            BCPGOutputStream         pgpOut = new BCPGOutputStream(bOut);
        
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
        out.writeObject(p);
        out.writeObject(q);
        out.writeObject(g);
        out.writeObject(y);
    }
    
    /**
     * @return g
     */
    public BigInteger getG()
    {
        return g.getValue();
    }

    /**
     * @return p
     */
    public BigInteger getP()
    {
        return p.getValue();
    }

    /**
     * @return q
     */
    public BigInteger getQ()
    {
        return q.getValue();
    }

    /**
     * @return g
     */
    public BigInteger getY()
    {
        return y.getValue();
    }

}
